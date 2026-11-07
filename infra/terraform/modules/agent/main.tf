terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

# The WhatsApp agent (repo srtomy-agent) runs on Cloud Run as its own service account. That account
# is its identity in front of the API: the API only lets it call /v1/internal/whatsapp/** and only
# with a Google ID token of this account (InternalSecurityConfig). It is not the account of the
# API, of the scheduler or of CI, so none of them can pose as the agent.
#
# Like the API, the Cloud Run service itself is not created here: the deploy workflow does it.

resource "google_service_account" "agent" {
  project      = var.project_id
  account_id   = "srtomy-agent"
  display_name = "WhatsApp agent: calls the API's WhatsApp endpoints"
}

# What only the agent reads. Names must match what the agent's deploy sets as environment secrets.
locals {
  secrets = {
    # Signs the webhook Kapso sends to the agent (a different one from the API's).
    "agent-kapso-webhook-secret" = var.kapso_webhook_secret
    "agent-anthropic-api-key"    = var.anthropic_api_key
    "agent-langsmith-api-key"    = var.langsmith_api_key
    # Where it keeps the conversations and the messages it already handled.
    "agent-database-url" = var.database_url
  }
}

resource "google_secret_manager_secret" "agent" {
  for_each = local.secrets

  project   = var.project_id
  secret_id = each.key

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "agent" {
  for_each = local.secrets

  secret      = google_secret_manager_secret.agent[each.key].id
  secret_data = each.value
}

resource "google_secret_manager_secret_iam_member" "agent_access" {
  for_each = local.secrets

  project   = var.project_id
  secret_id = google_secret_manager_secret.agent[each.key].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.agent.email}"
}

# The Kapso project key is shared with the API (it is the same project): one secret, two readers.
resource "google_secret_manager_secret_iam_member" "agent_reads_kapso_key" {
  project   = var.project_id
  secret_id = var.kapso_api_key_secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.agent.email}"
}

# --- Deploying the agent from its own repo -------------------------------------------------------
# Same idea as the cicd module (Workload Identity Federation, no JSON key), but with its own provider
# and its own deploy account, so the agent's repo cannot deploy the API and the API's repo cannot
# deploy the agent. The image goes to the same Artifact Registry repo as the API's.

resource "google_iam_workload_identity_pool_provider" "agent" {
  project                            = var.project_id
  workload_identity_pool_id          = var.workload_identity_pool_id
  workload_identity_pool_provider_id = "agent-github-actions"
  display_name                       = "GitHub Actions OIDC (agent)"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
    "attribute.ref"        = "assertion.ref"
  }

  attribute_condition = "assertion.repository == \"${var.github_repo}\" && assertion.ref == \"refs/heads/${var.deploy_branch}\""

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account" "agent_deploy" {
  project      = var.project_id
  account_id   = "gh-agent-deploy"
  display_name = "GitHub Actions deploy (srtomy-agent)"
  description  = "Impersonated by the agent's CI via Workload Identity Federation to push its image and deploy it to Cloud Run. Not used for anything else."
}

resource "google_service_account_iam_member" "agent_wif_impersonation" {
  service_account_id = google_service_account.agent_deploy.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${var.workload_identity_pool_name}/attribute.repository/${var.github_repo}"
}

resource "google_project_iam_member" "agent_deploy_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.agent_deploy.email}"
}

resource "google_project_iam_member" "agent_deploy_artifact_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.agent_deploy.email}"
}

# Deploying a service that runs as the agent's account needs permission to act as that account
# (only that one, not every account of the project).
resource "google_service_account_iam_member" "agent_deploy_acts_as_agent" {
  service_account_id = google_service_account.agent.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.agent_deploy.email}"
}
