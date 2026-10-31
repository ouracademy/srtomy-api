terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

# Names must match exactly what application-prod.yml references via
# ${sm@db-url}, ${sm@db-user}, ${sm@db-password}, ${sm@resend-api-key},
# ${sm@attendance-qr-secret}, ${sm@kapso-api-key}, ${sm@kapso-webhook-secret} and
# ${sm@whatsapp-delegation-key}.

# Signs the code in the attendance QR of an event. Generated here and kept in the state, so nobody
# has to invent or pass it around; replacing the resource changes it (open QR codes stop working).
resource "random_password" "attendance_qr_secret" {
  length  = 48
  special = false
}

# Signs the short-lived tokens the WhatsApp agent uses to call the API as a member (see
# DelegatedTokens in the API). Generated here, in PKCS#8 PEM as the API reads it, and kept in the
# state: nobody sees or types it. Replacing the resource changes the key, which only cuts the tokens
# in flight (they last ten minutes).
resource "tls_private_key" "whatsapp_delegation" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

locals {
  secrets = {
    "db-url"               = var.jdbc_url
    "db-user"              = var.database_user
    "db-password"          = var.database_password
    "resend-api-key"       = var.resend_api_key
    "attendance-qr-secret" = random_password.attendance_qr_secret.result

    # WhatsApp (specs 014 to 016).
    "kapso-api-key"           = var.kapso_api_key
    "kapso-webhook-secret"    = var.kapso_webhook_secret
    "whatsapp-delegation-key" = tls_private_key.whatsapp_delegation.private_key_pem_pkcs8
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

# application.yml also resolves auth0.clientId/clientSecret via sm:// — these
# secrets predate this module (created manually before the deploy pipeline
# existed) so we only grant access to them here, we don't manage their
# content with Terraform.
data "google_secret_manager_secret" "existing" {
  for_each = toset(var.existing_secret_ids)

  project   = var.project_id
  secret_id = each.value
}

resource "google_secret_manager_secret_iam_member" "existing_runtime_access" {
  for_each = toset(var.existing_secret_ids)

  project   = var.project_id
  secret_id = data.google_secret_manager_secret.existing[each.value].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${data.google_project.this.number}-compute@developer.gserviceaccount.com"
}
