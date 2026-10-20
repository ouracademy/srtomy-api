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

variable "existing_secret_ids" {
  description = "Secret Manager secrets that already exist outside this module (e.g. auth0-client-id/secret) which the Cloud Run runtime service account still needs read access to."
  type        = list(string)
  default     = []
}
