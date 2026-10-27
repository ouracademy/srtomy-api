terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

# The outbox delivers emails (and later WhatsApp messages) with one Cloud Tasks task per message:
# the task calls POST /v1/internal/outbox/{id}/attempt on the API, and the queue retries it while
# the API answers 503. Nothing runs when there is nothing to deliver, so the Cloud Run instance
# and the database can sleep.

data "google_project" "this" {
  project_id = var.project_id
}

locals {
  # The account Cloud Run runs the API as (the default Compute one, like in db-secrets).
  runtime_service_account = "${data.google_project.this.number}-compute@developer.gserviceaccount.com"
}

resource "google_project_service" "cloudtasks" {
  project            = var.project_id
  service            = "cloudtasks.googleapis.com"
  disable_on_destroy = false
}

resource "google_cloud_tasks_queue" "outbox" {
  for_each = var.channels

  project  = var.project_id
  location = var.region
  name     = "outbox-${each.value}"

  rate_limits {
    max_dispatches_per_second = 10
    max_concurrent_dispatches = 5
  }

  # 1, 2, 4, 8, 16, 32 minutes, then every hour; about four hours for 10 attempts, and never more
  # than 12: Resend remembers an Idempotency-Key for 24 hours, so a retry later than that could
  # send the email twice.
  retry_config {
    max_attempts       = var.max_attempts
    min_backoff        = "60s"
    max_backoff        = "3600s"
    max_doublings      = 5
    max_retry_duration = "43200s"
  }

  depends_on = [google_project_service.cloudtasks]
}

# The API creates the tasks.
resource "google_project_iam_member" "runtime_can_enqueue" {
  project = var.project_id
  role    = "roles/cloudtasks.enqueuer"
  member  = "serviceAccount:${local.runtime_service_account}"
}

# A task carries an ID token of the scheduler's account: whoever creates it must be allowed to act
# as that account.
resource "google_service_account_iam_member" "runtime_acts_as_caller" {
  service_account_id = "projects/${var.project_id}/serviceAccounts/${var.caller_service_account_id}"
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${local.runtime_service_account}"
}
