FROM amazoncorretto:17-alpine3.17

WORKDIR /app

COPY target/CloudStorage-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

#FROM maven:3.8.7 as builder
#WORKDIR /app
#COPY pom.xml .
#COPY src ./src
#RUN mvn package
#
#FROM amazoncorretto:17-alpine3.17
#WORKDIR /app
#COPY --from=builder /app/target/*.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]
