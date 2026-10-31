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
