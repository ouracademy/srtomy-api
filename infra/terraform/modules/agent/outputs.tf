output "agent_service_account_email" {
  description = "Set it as INTERNAL_AGENT_EMAIL in the API's Cloud Run service (the deploy workflow does it), and as the service account of the agent's Cloud Run service."
  value       = google_service_account.agent.email
}
