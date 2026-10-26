variable "project_id" {
  type = string
}

variable "region" {
  description = "Region of the Cloud Run service and of the scheduler job."
  type        = string
  default     = "southamerica-west1"
}

variable "service_name" {
  description = "The Cloud Run service the jobs call. Created by the deploy workflows, not by Terraform."
  type        = string
  default     = "sr-tommy-api"
}

variable "audience" {
  description = "The audience of the ID token the jobs send. The API checks the same value (internal.audience in application.yml); it does not have to be a real URL."
  type        = string
  default     = "https://api.srtommy.com/internal"
}

variable "outbox_schedule" {
  description = "How often the outbox is drained (cron). Every minute is also the delay of the first retry."
  type        = string
  default     = "* * * * *"
}
