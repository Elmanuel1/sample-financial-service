FROM azul/zulu-openjdk-alpine:21-jre-latest
# Set the environment variable for the jar file
RUN addgroup -S cross-border && adduser -S cross-border -G cross-border
USER cross-border:cross-border
COPY ./build/libs/cross-boarder-service.jar /app/service.jar
COPY flyway/ /app/flyway/
# Set the working directory to /app
WORKDIR /app

# Run the jar file when the container launches
ENTRYPOINT ["java", "-jar", "service.jar"]