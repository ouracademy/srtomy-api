variable "project_id" {
  type = string
}

variable "kapso_api_key_secret_id" {
  description = "Secret Manager id of the Kapso project key, created by the db-secrets module. The agent reads it to answer messages."
  type        = string
  default     = "kapso-api-key"
}

variable "kapso_webhook_secret" {
  description = "Secret of the Kapso webhook that points to the agent (POST /webhooks/kapso, event whatsapp.message.received). Kapso gives it when the webhook is created. Pass it with TF_VAR_agent_kapso_webhook_secret; never commit it."
  type        = string
  sensitive   = true
}

variable "anthropic_api_key" {
  description = "Anthropic API key, only when the agent calls Anthropic directly (MODEL_PROVIDER=anthropic). On Vertex AI (the default of the deploy) nothing reads it. Pass it with TF_VAR_agent_anthropic_api_key; never commit it."
  type        = string
  sensitive   = true
  default     = "not-set"
}

variable "langsmith_api_key" {
  description = "LangSmith API key for the traces (https://smith.langchain.com). The agent only sends traces when LANGSMITH_TRACING is true; decide region and retention first, they hold what members write. Pass it with TF_VAR_agent_langsmith_api_key; never commit it. A placeholder is fine while traces are off."
  type        = string
  sensitive   = true
  default     = "not-set"
}

variable "database_url" {
  description = "Connection string of the agent's own database (the neon module gives it). Without it the agent keeps its state in memory and loses a pending confirmation when it restarts."
  type        = string
  sensitive   = true
}

variable "workload_identity_pool_id" {
  description = "Id of the GitHub Actions pool created by the cicd module; the agent's repo gets its own provider in it."
  type        = string
}

variable "workload_identity_pool_name" {
  description = "Full resource name of that pool."
  type        = string
}

variable "github_repo" {
  description = "Repo of the agent allowed to deploy it, as \"owner/repo\"."
  type        = string
  default     = "ouracademy/srtomy-agent"
}

variable "deploy_branch" {
  description = "Only runs from this branch can get a deploy token."
  type        = string
  default     = "main"
}
