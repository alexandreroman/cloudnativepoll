# Copyright 2021 VMware. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

BACKEND_OCI_IMAGE  := ghcr.io/alexandreroman/cloudnativepoll-backend
WEBUI_OCI_IMAGE := ghcr.io/alexandreroman/cloudnativepoll-webui

clean:
	-rm -rf backend/target webui/target target

build: build-backend build-webui

deploy: deploy-backend deploy-webui

build-backend: backend/target/cloudnativepoll-backend.jar

backend/target/cloudnativepoll-backend.jar:
	./mvnw -B -pl fr.alexandreroman.cloudnativepoll:cloudnativepoll-backend clean package

deploy-backend: backend/target/cloudnativepoll-backend.jar
	./mvnw -B -pl fr.alexandreroman.cloudnativepoll:cloudnativepoll-backend spring-boot:build-image -DskipTests -Dspring-boot.build-image.imageName=${BACKEND_OCI_IMAGE}
	docker push -q ${BACKEND_OCI_IMAGE}

build-webui: webui/target/cloudnativepoll-webui.jar

webui/target/cloudnativepoll-webui.jar:
	./mvnw -B -pl fr.alexandreroman.cloudnativepoll:cloudnativepoll-webui clean package

deploy-webui: webui/target/cloudnativepoll-webui.jar
	./mvnw -B -pl fr.alexandreroman.cloudnativepoll:cloudnativepoll-webui spring-boot:build-image -DskipTests -Dspring-boot.build-image.imageName=${WEBUI_OCI_IMAGE}
	docker push -q ${WEBUI_OCI_IMAGE}
