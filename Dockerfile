FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/hl7-decoder-0.1.0-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
