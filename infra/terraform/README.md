# Infrastructure (Terraform)

Manages the GCP + Neon plumbing CI needs to build and deploy this app to Cloud Run: API
enablement, the Artifact Registry repo, Workload Identity Federation (WIF) so GitHub
Actions can authenticate without a long-lived JSON key, a Neon Postgres database, and the
Secret Manager secrets (`db-url`, `db-user`, `db-password`) the app reads at startup.

**Not managed here**: the Cloud Run service itself. It's created/updated by the deploy
workflows (`.github/workflows/deploy-dev.yml`, `deploy-prod.yml`) on every deploy, since
that's where the container image changes every time — having Terraform fight CI over the
image tag is more trouble than it's worth for a service this size.

## Layout

- `modules/cicd/` — WIF, Artifact Registry, the GitHub Actions deploy service account.
- `modules/neon/` — one Neon project (database + branch + role) per environment.
- `modules/db-secrets/` — wires the Neon module's outputs straight into Secret Manager,
  so the actual DB password never has to pass through a person's hands or a chat.
- `environments/dev/` — `sr-tommy-dev`.
- `environments/prod/` — `sr-tommy-prod`.

State lives remotely in a GCS bucket (`gs://sr-tommy-terraform-state`), one prefix per
environment, so it survives a wiped laptop and two people never clobber each other's
changes. The bucket itself was created manually once (not Terraform-managed — a backend
can't bootstrap its own storage):

```sh
gcloud storage buckets create gs://sr-tommy-terraform-state --project=sr-tommy-dev --location=us-central1 --uniform-bucket-level-access
gcloud storage buckets update gs://sr-tommy-terraform-state --versioning
```

## One-time setup (already done for sr-tommy-dev; still needed for sr-tommy-prod)

Project needs billing linked and these APIs enabled before Terraform can touch it:

```sh
gcloud billing projects link <PROJECT_ID> --billing-account=<BILLING_ACCOUNT_ID>
gcloud services enable run.googleapis.com artifactregistry.googleapis.com \
  iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com \
  secretmanager.googleapis.com --project=<PROJECT_ID>
```

**Neon API key**: generate one from the organization's settings
(`console.neon.tech/app/<org-id>/settings` → API Keys), not a personal account's — the
org-scoped key type specifically (there's a project-scoped type too, which cannot create
new projects and fails with a 401/404). Never commit it; export it as `NEON_API_KEY` in
your shell before running Terraform:

```sh
export NEON_API_KEY="..."
```

## Day to day

```sh
cd infra/terraform/environments/dev   # or environments/prod
terraform init
terraform plan    # review before applying — nothing above costs much, but review anyway
terraform apply
```

Switch GCP account first if needed: `gcloud auth application-default login
--account=<account>` (Terraform's Google provider uses Application Default Credentials,
the same ones `gcloud` uses), then `gcloud auth application-default set-quota-project
<PROJECT_ID>` if it picks the wrong one.

### Neon free-tier quirks hit while setting this up

- `history_retention_seconds` defaults (in the provider) to a paid-plan value (24h); the
  free tier caps it at 21600 (6h) — already set correctly in `modules/neon`, but worth
  knowing if this ever needs changing.
- The free tier does not allow setting `primary_compute.suspend_timeout_seconds` via the
  API at all ("modifying the suspend interval is not permitted on this account") — the
  module intentionally doesn't set it; the account default (5 minutes) applies.
- If `db-url`/`db-user`/`db-password` already exist in Secret Manager from an older setup,
  `google_secret_manager_secret.db["<name>"]` creation 409s. Import the existing secret
  container instead of deleting it — the next `apply` then creates a fresh *version* with
  the new (Neon) value, which is what the app actually reads:
  ```sh
  terraform import 'module.db_secrets.google_secret_manager_secret.db["db-url"]' projects/<PROJECT_NUMBER>/secrets/db-url
  ```

### If `terraform apply`/`plan` ever fails with "Error acquiring the state lock"

Usually means a previous `apply` got interrupted (closed terminal, lost connection) before
it finished, leaving the GCS lock file behind. Confirm nothing is actually still running,
then:

```sh
terraform force-unlock <LOCK_ID>   # LOCK_ID is printed in the error message
```

## Wiring up GitHub Actions

After `terraform apply` on an environment, set these as repo variables (not secrets —
none of these values are sensitive on their own):

```sh
terraform output workload_identity_provider
terraform output deploy_service_account_email
terraform output artifact_registry_repository_url
```

```sh
gh variable set <ENV>_WORKLOAD_IDENTITY_PROVIDER --repo ouracademy/srtomy-api --body "<value>"
gh variable set <ENV>_DEPLOY_SERVICE_ACCOUNT --repo ouracademy/srtomy-api --body "<value>"
gh variable set <ENV>_ARTIFACT_REGISTRY_URL --repo ouracademy/srtomy-api --body "<value>"
```

Where `<ENV>` is `DEV` or `PROD`, matching what `.github/workflows/deploy-dev.yml` and
`deploy-prod.yml` reference. `DEV_*` is already set.

For `deploy-prod.yml` specifically: it targets a GitHub Environment named `production`,
meant to be gated by a manual approval step. Configure that once in the GitHub UI —
Settings → Environments → New environment → `production` → add yourself (or whoever
should approve deploys) under "Required reviewers". Without that rule, the workflow still
works, it just runs immediately with no approval gate when someone clicks "Run workflow".
