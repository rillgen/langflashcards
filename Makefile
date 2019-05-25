IMAGE=rillgen/fcardbot
TAG?=latest

package:
	mvn clean package -DskipTests
	docker build -t $(IMAGE):$(TAG) .