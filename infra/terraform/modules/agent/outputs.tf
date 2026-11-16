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

# The portal chat. The Cloud Run service is not in Terraform: these go to the agent's deploy as
# repository variables of srtomy-agent (DEV_AUTH0_DOMAIN, DEV_AUTH0_AUDIENCE, DEV_CORS_ORIGINS).
output "auth0_domain" {
  description = "Repository variable DEV_AUTH0_DOMAIN of srtomy-agent (AUTH0_DOMAIN)."
  value       = var.auth0_domain
}

output "auth0_audience" {
  description = "Repository variable DEV_AUTH0_AUDIENCE of srtomy-agent (AUTH0_AUDIENCE)."
  value       = var.auth0_audience
}

output "cors_origins" {
  description = "Repository variable DEV_CORS_ORIGINS of srtomy-agent (CORS_ORIGINS), comma-separated."
  value       = join(",", var.cors_origins)
}

output "tool_approval_secret_id" {
  description = "Secret Manager id the agent's deploy mounts as TOOL_APPROVAL_SECRET."
  value       = google_secret_manager_secret.agent["agent-tool-approval-secret"].secret_id
}
