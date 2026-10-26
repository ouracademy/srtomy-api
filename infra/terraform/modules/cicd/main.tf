terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

locals {
  required_apis = [
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
    "sts.googleapis.com",
    "secretmanager.googleapis.com",
  ]
}

resource "google_project_service" "required" {
  for_each = toset(local.required_apis)

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "images" {
  project       = var.project_id
  location      = var.region
  repository_id = var.artifact_registry_repository_id
  format        = "DOCKER"
  description   = "Container images for ${var.service_name}, built by CI and deployed to Cloud Run."

  depends_on = [google_project_service.required]
}

# Lets GitHub Actions authenticate as a GCP service account via OIDC token
# exchange — no long-lived JSON key ever leaves GCP or gets stored as a
# GitHub secret.
resource "google_iam_workload_identity_pool" "github" {
  project                   = var.project_id
  workload_identity_pool_id = "github-actions"
  display_name              = "GitHub Actions"
  description               = "Used by google-github-actions/auth in CI to deploy ${var.service_name}."

  depends_on = [google_project_service.required]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  project                            = var.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = "github-actions"
  display_name                       = "GitHub Actions OIDC"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
    "attribute.ref"        = "assertion.ref"
  }

  # Only this exact repo AND this exact branch can mint a deploy token — a
  # pull request from a fork (or any other branch/repo) cannot.
  attribute_condition = "assertion.repository == \"${var.github_repo}\" && assertion.ref == \"refs/heads/${var.deploy_branch}\""

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account" "deploy" {
  project      = var.project_id
  account_id   = "gh-actions-deploy"
  display_name = "GitHub Actions deploy (${var.service_name})"
  description  = "Impersonated by CI via Workload Identity Federation to build/push images and deploy to Cloud Run. Not used for anything else."
}

resource "google_service_account_iam_member" "wif_impersonation" {
  service_account_id = google_service_account.deploy.name
  role                = "roles/iam.workloadIdentityUser"
  member              = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository/${var.github_repo}"
}

resource "google_project_iam_member" "deploy_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.deploy.email}"
}

resource "google_project_iam_member" "deploy_artifact_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.deploy.email}"
}

# Deploying to Cloud Run requires permission to act as the service's own
# runtime identity (the default compute SA, unless/until ovejafiel-api gets a
# dedicated runtime SA).
resource "google_project_iam_member" "deploy_service_account_user" {
  project = var.project_id
  role    = "roles/iam.serviceAccountUser"
  member  = "serviceAccount:${google_service_account.deploy.email}"
}
