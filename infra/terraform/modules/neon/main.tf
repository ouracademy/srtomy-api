terraform {
  required_providers {
    neon = {
      source  = "kislerdm/neon"
      version = "~> 0.14"
    }
  }
}

# Provider itself is configured once in each environment's root module (reads
# NEON_API_KEY from the environment there) — a module shouldn't own its own
# provider block, only declare that it needs one.

resource "neon_project" "db" {
  name       = var.project_name
  org_id     = var.org_id
  region_id  = var.region_id
  pg_version = var.pg_version

  # Free tier caps point-in-time-restore history at 6h (21600s); the
  # provider's own default (24h) is a paid-plan value and gets rejected here.
  history_retention_seconds = 21600

  branch {
    database_name = var.database_name
    role_name     = var.role_name
  }

  # Not setting primary_compute/suspend_timeout_seconds: the free tier
  # doesn't allow changing it via the API ("modifying the suspend interval is
  # not permitted on this account") — it suspends 5 minutes after the last
  # active connection by default anyway, which is exactly what we want here.
}
