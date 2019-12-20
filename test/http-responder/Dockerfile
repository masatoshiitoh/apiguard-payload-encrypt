FROM openjdk:12-jdk-alpine AS build-env
VOLUME /tmp
# ARG JAR_FILE
# COPY ${JAR_FILE} app.jar

RUN apk update && apk upgrade && apk add --no-cache git
RUN git clone https://github.com/masatoshiitoh/http-responder.git
RUN cd http-responder ; chmod a+x ./gradlew
RUN cd http-responder ; ./gradlew shadowJar

FROM openjdk:12-jdk-alpine
COPY --from=build-env /http-responder/build/libs/apiguard-1.0.0-SNAPSHOT-fat.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
