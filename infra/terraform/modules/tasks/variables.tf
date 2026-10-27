variable "project_id" {
  type = string
}

variable "region" {
  description = "Region of the Cloud Run service the tasks call (part of its URL)."
  type        = string
  default     = "southamerica-west1"
}

variable "queue_region" {
  description = "Region where the queues live. Cloud Tasks is not available in southamerica-west1 (Santiago); a task only calls a URL, so it can be in another region: São Paulo is the closest. The API must be told the same one (TASKS_LOCATION). Check the list with: gcloud tasks locations list"
  type        = string
  default     = "southamerica-east1"
}

variable "service_name" {
  description = "The Cloud Run service the tasks call. Created by the deploy workflows, not by Terraform."
  type        = string
  default     = "sr-tommy-api"
}

variable "caller_service_account_id" {
  description = "The service account whose ID token the tasks carry (the scheduler's: the API only accepts that one). The Cloud Run runtime account is allowed to act as it."
  type        = string
}

variable "channels" {
  description = "One queue per delivery channel of the outbox (outbox-<channel>), so each has its own rate limit and retry policy. Add whatsapp when that channel exists."
  type        = set(string)
  default     = ["email"]
}

variable "max_attempts" {
  description = "Attempts before a task is given up. The API gives the message up after the same number (outbox.max-attempts in application-prod.yml): keep them equal."
  type        = number
  default     = 10
}
