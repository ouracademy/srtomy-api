terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
    neon = {
      source  = "kislerdm/neon"
      version = "~> 0.14"
    }
  }

  backend "gcs" {
    bucket = "sr-tommy-terraform-state"
    prefix = "terraform/state/dev"
  }
}

provider "google" {
  project = "sr-tommy-dev"
  region  = "southamerica-west1"
}

# Reads the API key from the NEON_API_KEY environment variable — never commit
# it. Generate one from the organization's settings, not a personal account's
# (an org-scoped project needs an org-scoped key).
provider "neon" {}

# Export it as TF_VAR_resend_api_key (never commit it).
variable "resend_api_key" {
  type      = string
  sensitive = true
}

module "cicd" {
  source = "../../modules/cicd"

  project_id = "sr-tommy-dev"
}

module "neon" {
  source = "../../modules/neon"

  project_name = "sr-tommy-dev"
  org_id       = "org-blue-night-91089070"
}

module "db_secrets" {
  source = "../../modules/db-secrets"

  project_id          = "sr-tommy-dev"
  jdbc_url            = module.neon.jdbc_url
  database_user       = module.neon.database_user
  database_password   = module.neon.database_password
  resend_api_key      = var.resend_api_key
  existing_secret_ids = ["auth0-client-id", "auth0-client-secret"]
}

# Cloud Scheduler calls the API's internal endpoints (the outbox that sends emails). The account's
# email is set as INTERNAL_SCHEDULER_EMAIL in the deploy workflow.
module "scheduler" {
  source = "../../modules/scheduler"

  project_id = "sr-tommy-dev"
}

output "workload_identity_provider" {
  value = module.cicd.workload_identity_provider
}

output "deploy_service_account_email" {
  value = module.cicd.deploy_service_account_email
}

output "artifact_registry_repository_url" {
  value = module.cicd.artifact_registry_repository_url
}

output "neon_database_host" {
  value = module.neon.database_host
}

output "scheduler_service_account_email" {
  value = module.scheduler.scheduler_service_account_email
}
