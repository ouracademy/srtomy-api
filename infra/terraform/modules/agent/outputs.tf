output "agent_service_account_email" {
  description = "Set it as INTERNAL_AGENT_EMAIL in the API's Cloud Run service (the deploy workflow does it), and as the service account of the agent's Cloud Run service."
  value       = google_service_account.agent.email
}

output "deploy_workload_identity_provider" {
  description = "Repository variable DEV_WORKLOAD_IDENTITY_PROVIDER of srtomy-agent."
  value       = google_iam_workload_identity_pool_provider.agent.name
}

output "deploy_service_account_email" {
  description = "Repository variable DEV_DEPLOY_SERVICE_ACCOUNT of srtomy-agent."
  value       = google_service_account.agent_deploy.email
}
