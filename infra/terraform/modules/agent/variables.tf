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
  description = "Anthropic API key of the agent's model (https://console.anthropic.com). Pass it with TF_VAR_agent_anthropic_api_key; never commit it."
  type        = string
  sensitive   = true
}

variable "langsmith_api_key" {
  description = "LangSmith API key for the traces (https://smith.langchain.com). The agent only sends traces when LANGSMITH_TRACING is true; decide region and retention first, they hold what members write. Pass it with TF_VAR_agent_langsmith_api_key; never commit it. A placeholder is fine while traces are off."
  type        = string
  sensitive   = true
  default     = "not-set"
}
