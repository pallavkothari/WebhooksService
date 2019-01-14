build:
	mvn install -DskipTests
	docker build . -t scheduler
	docker tag scheduler:latest pallavkothari/scheduler:latest


run:
	docker run --name scheduler --network=host -h=127.0.0.1 --rm -it scheduler:latest

push:
	docker push pallavkothari/scheduler:latest
