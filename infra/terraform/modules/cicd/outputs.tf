output "workload_identity_provider" {
  description = "Full resource name to pass as workload_identity_provider to google-github-actions/auth."
  value       = google_iam_workload_identity_pool_provider.github.name
}

output "deploy_service_account_email" {
  description = "Email to pass as service_account to google-github-actions/auth."
  value       = google_service_account.deploy.email
}

output "artifact_registry_repository_url" {
  description = "Base URL to tag and push images to, e.g. <url>/sr-tommy-api:<tag>."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.images.repository_id}"
}
