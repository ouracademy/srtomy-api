variable "project_id" {
  type = string
}

variable "region" {
  description = "Region of the Cloud Run service and of the scheduler jobs."
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

variable "jobs" {
  description = "One Cloud Scheduler job per internal endpoint of the API (POST /v1/internal/...). The key is the job's name."
  type = map(object({
    description = string
    schedule    = string
    path        = string
  }))
  default = {
    "drain-outbox" = {
      description = "Retries the emails that did not leave right after their commit (the first attempt is immediate, so this is only the safety net)."
      schedule    = "* * * * *"
      path        = "/v1/internal/outbox/drain"
    }
    "extend-event-series" = {
      description = "Adds upcoming occurrences to the event series that never end (or end far away)."
      schedule    = "30 3 * * *"
      path        = "/v1/internal/events/extend-series"
    }
  }
}
