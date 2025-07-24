FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]