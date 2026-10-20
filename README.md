# Sr. Tommy

Un sistema para iglesias

## Setup

- Install https://sdkman.io/
- Install java con sdkman: `sdk install java 21.0.0.0-amzn`
- Use java: `sdk use java 21.0.0.0-amzn`
- Create BD: `docker-compose up -d`
- Check Authenticate and then run: `export GOOGLE_APPLICATION_CREDENTIALS="path-to/application_default_credentials.json`
- Run the app: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Enable the repo's git hooks (Conventional Commits check, one-time per clone): `git config core.hooksPath .githooks`

See [AGENTS.md](./AGENTS.md) for commit message, code style and language conventions.

## Google Cloud Platform

### Credentials authenticate

<!-- arthur only -->

`gcloud auth application-default login`

<!-- for other devs -->

`gcloud auth application-default login --impersonate-service-account sr-tommy-dev-api@sr-tommy-dev.iam.gserviceaccount.com`

### Deploy to Cloud Run

Deploys are automated via GitHub Actions — see `.github/workflows/deploy-dev.yml`
(auto, on every push to `main` once `ci` passes) and `.github/workflows/deploy-prod.yml`
(manual, `workflow_dispatch`, gated by the `production` GitHub Environment). The GCP
infrastructure these workflows use (Workload Identity Federation, Artifact Registry,
the deploy service account, Secret Manager) is provisioned by Terraform — see
`infra/terraform/README.md` for setup and troubleshooting.

There should be no need to deploy by hand, but if you ever do (e.g. to debug the image
build locally), it matches what the workflow runs:

```sh
export IMAGE="southamerica-west1-docker.pkg.dev/sr-tommy-dev/sr-tommy-api/sr-tommy-api:local"
gcloud auth configure-docker southamerica-west1-docker.pkg.dev
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=${IMAGE} \
  -Dspring-boot.build-image.builder=gcr.io/buildpacks/builder
docker push ${IMAGE}
gcloud run deploy sr-tommy-api --image ${IMAGE} --region southamerica-west1 --project sr-tommy-dev
```
