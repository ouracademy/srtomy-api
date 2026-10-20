terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

# Names must match exactly what application-prod.yml references via
# ${sm://db-url}, ${sm://db-user}, ${sm://db-password}.
locals {
  secrets = {
    "db-url"      = var.jdbc_url
    "db-user"     = var.database_user
    "db-password" = var.database_password
  }
}

resource "google_secret_manager_secret" "db" {
  for_each = local.secrets

  project   = var.project_id
  secret_id = each.key

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db" {
  for_each = local.secrets

  secret      = google_secret_manager_secret.db[each.key].id
  secret_data = each.value
}

# Cloud Run defaults to running as the project's default compute service
# account unless the deploy workflow specifies a different one — grant it
# read access so spring.config.import: sm:// can actually resolve these at
# startup.
data "google_project" "this" {
  project_id = var.project_id
}

resource "google_secret_manager_secret_iam_member" "runtime_access" {
  for_each = local.secrets

  project   = var.project_id
  secret_id = google_secret_manager_secret.db[each.key].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${data.google_project.this.number}-compute@developer.gserviceaccount.com"
}
