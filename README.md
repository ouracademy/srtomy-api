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

### Deploy to appengine

`./mvnw -DskipTests package appengine:deploy`

### Deploy to cloudrun

```sh
export PROJECT_ID=$(gcloud config get-value project)
gcloud auth configure-docker
gcloud auth configure-docker southamerica-west1-docker.pkg.dev
docker push gcr.io/${PROJECT_ID}/sr-tommy-api
```

```sh
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=gcr.io/${PROJECT_ID}/sr-tommy-api \
  -Dspring-boot.build-image.builder=gcr.io/buildpacks/builder
```
