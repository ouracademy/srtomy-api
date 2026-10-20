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
