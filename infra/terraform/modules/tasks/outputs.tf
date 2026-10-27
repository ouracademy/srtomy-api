output "api_url" {
  description = "The URL of the Cloud Run service the tasks call. Set it as the GitHub variable DEV_API_URL / PROD_API_URL: the deploy workflow passes it to the API as TASKS_TARGET_URL."
  value       = "https://${var.service_name}-${data.google_project.this.number}.${var.region}.run.app"
}
