FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
WORKDIR /app

# Accept build arguments
ARG JWT_SECRET
ARG MONGODB_CONNECTION_STRING

# Set as environment variables
ENV JWT_SECRET=${JWT_SECRET}
ENV MONGODB_CONNECTION_STRING=${MONGODB_CONNECTION_STRING}

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]