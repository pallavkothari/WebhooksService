build:
	mvn install -DskipTests
	docker build . -t scheduler

run:
	docker run --name scheduler -p 8080:8080 --rm -it scheduler:latest