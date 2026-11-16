variable "project_id" {
  type = string
}

variable "jdbc_url" {
  type      = string
  sensitive = true
}

variable "database_user" {
  type      = string
  sensitive = true
}

variable "database_password" {
  type      = string
  sensitive = true
}

variable "resend_api_key" {
  description = "Resend API key used to send invitation and sharing emails (https://resend.com/api-keys). Pass it with TF_VAR_resend_api_key; never commit it."
  type        = string
  sensitive   = true
}

variable "kapso_api_key" {
  description = "Project API key of Kapso (Kapso: Integrations > API keys). It sends WhatsApp messages and creates the connection links of the churches. Pass it with TF_VAR_kapso_api_key; never commit it."
  type        = string
  sensitive   = true
}

variable "kapso_webhook_secret" {
  description = "Secret Kapso generates for the project webhook that points to the API (POST /v1/webhooks/whatsapp, event whatsapp.phone_number.created). Kapso gives it when the webhook is created. Pass it with TF_VAR_kapso_webhook_secret; never commit it."
  type        = string
  sensitive   = true
}

variable "dlocalgo_api_key" {
  description = "API key of dLocal Go (dashboard: Integrations > API keys; the sandbox one in dev). It creates and reads the payments of the paid plan. Pass it with TF_VAR_dlocalgo_api_key; never commit it."
  type        = string
  sensitive   = true
}

variable "dlocalgo_secret_key" {
  description = "Secret key of dLocal Go, next to the API key. It authenticates the calls and checks the signature of its payment notifications. Pass it with TF_VAR_dlocalgo_secret_key; never commit it."
  type        = string
  sensitive   = true
}

variable "existing_secret_ids" {
  description = "Secret Manager secrets that already exist outside this module (e.g. auth0-client-id/secret) which the Cloud Run runtime service account still needs read access to."
  type        = list(string)
  default     = []
}
