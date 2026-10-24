-- A church cannot have two live custom roles with the same name, ignoring case.
CREATE UNIQUE INDEX iam_role_church_name_idx
    ON iam_role (church_id, lower(name))
    WHERE church_id IS NOT NULL AND deleted_time IS NULL;
