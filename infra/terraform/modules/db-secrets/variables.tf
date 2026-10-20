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
