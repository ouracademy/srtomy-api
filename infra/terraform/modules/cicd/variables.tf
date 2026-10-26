variable "project_id" {
  description = "GCP project this environment's Cloud Run service, Artifact Registry repo and CI/CD identity live in."
  type        = string
}

variable "region" {
  description = "Region for the Artifact Registry repo and Cloud Run service."
  type        = string
  default     = "southamerica-west1"
}

variable "github_repo" {
  description = "GitHub repo allowed to impersonate the deploy service account, as \"owner/repo\"."
  type        = string
  default     = "ouracademy/ovejafiel-api"
}

variable "deploy_branch" {
  description = "Only workflow runs triggered from this branch can obtain a deploy token — keeps pull request branches and forks from deploying."
  type        = string
  default     = "main"
}

variable "artifact_registry_repository_id" {
  description = "Name of the Artifact Registry Docker repository."
  type        = string
  default     = "sr-tommy-api"
}

variable "service_name" {
  description = "Cloud Run service name."
  type        = string
  default     = "sr-tommy-api"
}
