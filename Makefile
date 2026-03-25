.PHONY: clean build run test

APP_NAME := java-spring-boot-sql2o-rest-crud
JAR_FILE := target/$(APP_NAME)-1.0-SNAPSHOT.jar

clean:
	mvn clean

build: clean
	mvn package -DskipTests

run: build
	java -jar $(JAR_FILE)

test:
	mvn test
