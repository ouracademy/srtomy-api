output "scheduler_service_account_email" {
  description = "Set it as INTERNAL_SCHEDULER_EMAIL in the Cloud Run service: the API only accepts tokens of this account."
  value       = google_service_account.scheduler.email
}

output "audience" {
  value = var.audience
}
