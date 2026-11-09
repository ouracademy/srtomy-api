output "database_host" {
  value = neon_project.db.database_host
}

output "database_name" {
  value = neon_project.db.database_name
}

output "database_user" {
  value = neon_project.db.database_user
}

output "database_password" {
  value     = neon_project.db.database_password
  sensitive = true
}

output "jdbc_url" {
  description = "Value for the db-url secret — Spring's spring.datasource.url expects a JDBC URL, not a bare connection string."
  value       = "jdbc:postgresql://${neon_project.db.database_host}/${neon_project.db.database_name}?sslmode=require"
}

output "agent_database_url" {
  description = "Connection string of the agent's database (what psycopg reads), or empty when there is none."
  sensitive   = true
  value       = var.agent_database_name == "" ? "" : "postgresql://${neon_project.db.database_user}:${neon_project.db.database_password}@${neon_project.db.database_host}/${var.agent_database_name}?sslmode=require"
}
