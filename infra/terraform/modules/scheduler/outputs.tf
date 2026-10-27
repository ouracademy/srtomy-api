output "scheduler_service_account_email" {
  description = "Set it as INTERNAL_SCHEDULER_EMAIL in the Cloud Run service: the API only accepts tokens of this account."
  value       = google_service_account.scheduler.email
}

output "scheduler_service_account_id" {
  description = "The account's email, as the tasks module and IAM bindings name it."
  value       = google_service_account.scheduler.email
}

output "audience" {
  value = var.audience
}
