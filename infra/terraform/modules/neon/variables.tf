variable "project_name" {
  description = "Name for the Neon project (shows up in Neon's dashboard)."
  type        = string
}

variable "org_id" {
  description = "Neon organization ID. Required for an org-owned account — omitting it can make Terraform think the project belongs somewhere else and try to destroy/recreate it."
  type        = string
}

variable "region_id" {
  description = "Neon region. Neon has no South America region; aws-us-east-2 (Ohio) is the closest standard option."
  type        = string
  default     = "aws-us-east-2"
}

variable "pg_version" {
  description = "Postgres major version — kept aligned with docker-compose.yml's postgres:16-alpine."
  type        = number
  default     = 16
}

variable "database_name" {
  type    = string
  default = "srtommy"
}

variable "role_name" {
  type    = string
  default = "srtommy"
}
