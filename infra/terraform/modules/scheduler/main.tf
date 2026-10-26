terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

# The API never schedules work on its own (no @Scheduled): Cloud Run may have no instance running,
# and with more than one instance every one would fire. Cloud Scheduler calls an internal endpoint
# instead, with a Google ID token of the service account below; the API checks that token (see
# InternalSecurityConfig).

data "google_project" "this" {
  project_id = var.project_id
}

resource "google_project_service" "scheduler" {
  project            = var.project_id
  service            = "cloudscheduler.googleapis.com"
  disable_on_destroy = false
}

resource "google_service_account" "scheduler" {
  project      = var.project_id
  account_id   = "outbox-scheduler"
  display_name = "Cloud Scheduler: calls the API's internal endpoints"
}

# Lets Cloud Scheduler mint ID tokens as that service account.
resource "google_service_account_iam_member" "scheduler_agent_can_mint_tokens" {
  service_account_id = google_service_account.scheduler.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:service-${data.google_project.this.number}@gcp-sa-cloudscheduler.iam.gserviceaccount.com"

  depends_on = [google_project_service.scheduler]
}

resource "google_cloud_scheduler_job" "drain_outbox" {
  project     = var.project_id
  region      = var.region
  name        = "drain-outbox"
  description = "Sends the emails that are due (and retries the ones that failed)."
  schedule    = var.outbox_schedule
  time_zone   = "Etc/UTC"

  # A slow provider must not stack runs: the next tick is a minute away.
  attempt_deadline = "60s"

  retry_config {
    retry_count = 0
  }

  http_target {
    http_method = "POST"
    # The URL Cloud Run gives every service: https://<service>-<project number>.<region>.run.app
    uri = "https://${var.service_name}-${data.google_project.this.number}.${var.region}.run.app/v1/internal/outbox/drain"

    oidc_token {
      service_account_email = google_service_account.scheduler.email
      audience              = var.audience
    }
  }

  depends_on = [
    google_project_service.scheduler,
    google_service_account_iam_member.scheduler_agent_can_mint_tokens,
  ]
}
