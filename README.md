# Sr. Tommy

Un sistema para iglesias

## Setup

- Install https://sdkman.io/
- Install java 21 con sdkman: `sdk install java 21.ea.35-open`
- Use java 21: `sdk use java 21.ea.35-open`
- Create BD: `docker-compose up -d`
- Run the app: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

## Google Cloud Platform

### Credentials authenticate

`gcloud auth application-default login --impersonate-service-account farmacia-unmsm-dev-app@farmacia-unmsm-dev.iam.gserviceaccount.com`

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
