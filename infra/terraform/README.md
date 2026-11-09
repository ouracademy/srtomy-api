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
- `modules/scheduler/` — Cloud Scheduler jobs that call the API's internal endpoints (`POST /v1/internal/...`) with an ID
  token of the `api-scheduler` service account: extend the event series and sweep the outbox, both
  daily. Add a job in `var.jobs` for any new calendar work.
- `modules/tasks/` — the Cloud Tasks queues of the outbox (`outbox-email`, ...), the retry policy, and the
  permissions the API needs to create tasks.
- `modules/agent/` — the WhatsApp agent's service account and its own secrets (Anthropic, LangSmith, and the secret of the webhook Kapso sends it).
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
export TF_VAR_resend_api_key="re_..."   # Resend API key (invitation and sharing emails)
```

**WhatsApp** (specs `014` to `016`; the agent is the repo `ouracademy/srtomy-agent`). Export these too, never commit them:

```sh
export TF_VAR_kapso_api_key="..."                  # Kapso: Integrations > API keys
export TF_VAR_kapso_webhook_secret="..."           # given by Kapso when you create the project webhook (below)
export TF_VAR_agent_kapso_webhook_secret="$(openssl rand -hex 32)"  # you choose it: keep it, Kapso gets the same one
export TF_VAR_agent_langsmith_api_key="..."        # optional: leave unset while traces are off
# TF_VAR_agent_anthropic_api_key only if the agent calls Anthropic directly; the default is Claude on Vertex AI.
```

There are two kinds of webhook in Kapso, and only the first is made by hand:

| Webhook | Made by | URL | Events |
| --- | --- | --- | --- |
| **Project** (one) | You, in Kapso: Integrations > Webhooks > Platform webhooks > Add webhook, payload version **v2** | `https://<api>/v1/webhooks/whatsapp` | `whatsapp.phone_number.created`, `.deleted`, `.offboarded`, `.disconnected`, `.reconnected`, `whatsapp.account.disabled`, `.reinstated`, `.restricted`, `.violation` |
| **Per number** (one for each church) | **The API**, when a church connects: it calls Kapso to make it | `https://<agent>/webhooks/kapso` | `whatsapp.message.received` |

Kapso shows the secret of the project webhook when you create it (copy it to `TF_VAR_kapso_webhook_secret`). The secret of the per-number webhook is the one you chose (`TF_VAR_agent_kapso_webhook_secret`): the API reads it from Secret Manager and gives it to Kapso, and the agent checks the signature with it. Set `AGENT_WEBHOOK_URL` for the API through the repository variable `DEV_AGENT_WEBHOOK_URL` / `PROD_AGENT_WEBHOOK_URL` (the public URL of the agent's Cloud Run service). A number that was connected before this was set has no webhook: make it by hand once (`POST /platform/v1/whatsapp/phone_numbers/{id}/webhooks`, see `docs.kapso.ai`) or press "Connect" again.

When Kapso says a number was deleted, offboarded or disconnected, or the account disabled, the API marks the connection as disconnected (with the reason) and everything that sends or answers stops; the portal shows it and offers to connect again. A number the pastor disconnected from the portal is never brought back by Kapso's "reconnected".

What Terraform now creates for them:

- In Secret Manager, read by the API: `kapso-api-key`, `kapso-webhook-secret` and `whatsapp-delegation-key`. The last one is **generated** (a 2048-bit RSA key in PKCS#8 PEM, kept in the state) and signs the ten-minute tokens the agent uses to call the API as a member; nobody needs to see it. Replacing it only cuts the tokens in flight.
- `modules/agent/`: the `srtomy-agent` service account (the only one the API lets call `/v1/internal/whatsapp/**`) and its own secrets `agent-kapso-webhook-secret` (also read by the API), `agent-anthropic-api-key` (unused on Vertex), `agent-langsmith-api-key` and `agent-database-url`. The last one points to a second database (`srtomy_agent`) that the `neon` module creates in the same Neon project, where the agent keeps its conversations (a separate database, not a separate credential: it uses the API's role); it also reads the Kapso project key `kapso-api-key`.
- The deploy workflows of the API pass `INTERNAL_AGENT_EMAIL=srtomy-agent@<project>.iam.gserviceaccount.com`, so the API accepts that account's Google ID tokens. Redeploy the API after the first `apply`.

`modules/agent/` also turns on `aiplatform.googleapis.com` and gives the agent's account `roles/aiplatform.user`, so it calls Claude on Vertex AI with no key. One manual step remains: enable Claude in Model Garden for the project (console > Model Garden > Claude Sonnet 5.5 > Enable; it accepts Anthropic's terms).

The agent deploys itself from its own repo (`.github/workflows/deploy-dev.yml` there, Google buildpacks, no Dockerfile). `modules/agent/` also creates what that needs: a provider `agent-github-actions` in the same Workload Identity pool (only `ouracademy/srtomy-agent` on `main`) and the account `gh-agent-deploy` (Cloud Run admin, Artifact Registry writer, and permission to act as `srtomy-agent` only). After `apply`, set these variables in the agent's repo (Settings > Variables): `DEV_WORKLOAD_IDENTITY_PROVIDER` = output `agent_deploy_workload_identity_provider`, `DEV_DEPLOY_SERVICE_ACCOUNT` = output `agent_deploy_service_account_email`, `DEV_ARTIFACT_REGISTRY_URL` = output `artifact_registry_repository_url`, and `DEV_API_URL` = the API's URL.

## Cloud Scheduler and Cloud Tasks

After the first `terraform apply` (dev or prod):

1. Copy the `api_url` output into the GitHub repository variable `DEV_API_URL` (or `PROD_API_URL`): the deploy workflow passes it to the API as `TASKS_TARGET_URL`. Without it the API delivers each message once, right after the commit, and never retries.
2. Redeploy the API, so Cloud Run gets `INTERNAL_SCHEDULER_EMAIL` (the API only accepts ID tokens of that account), `TASKS_PROJECT` and `TASKS_TARGET_URL`.

`modules/scheduler/` creates the `api-scheduler` service account and one job per entry of `var.jobs`. The jobs live in `southamerica-east1` because Cloud Scheduler does not exist in `southamerica-west1`; they still call the API in Santiago.
`modules/tasks/` creates one queue per channel, in `southamerica-east1` for the same reason (`TASKS_LOCATION` in the deploy workflows must match); its `max_attempts` must equal `outbox.max-attempts` in
`application-prod.yml`.

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
