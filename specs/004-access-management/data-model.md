# Data model: 004-access-management

Migration `V1.2.0__custom_roles.sql`:

- Partial unique index `iam_role_church_name_idx` on `(church_id, lower(name)) WHERE church_id IS NOT NULL AND deleted_time IS NULL`, a safety net for the case-insensitive name rule.

No other schema change: `iam_role` (`church_id`, `deleted_time`, `permissions text[]`) and `role_binding` (`granted_by`) came with 003.
