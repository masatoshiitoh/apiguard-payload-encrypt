FROM openjdk:12-jdk-alpine
VOLUME /tmp
# ARG JAR_FILE
# COPY ${JAR_FILE} app.jar
COPY build/libs/apiguard-1.0.0-SNAPSHOT-fat.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

#FROM openjdk:12-jdk-alpine AS build-env
#
#RUN apk update && apk upgrade && apk add --no-cache git
#RUN git clone https://github.com/masatoshiitoh/apiguard-payload-encrypt.git
#RUN cd apiguard-payload-encrypt ; chmod a+x ./gradlew
#RUN cd apiguard-payload-encrypt ; ./gradlew shadowJar
#
#FROM openjdk:12-jdk-alpine
#COPY --from=build-env /apiguard-payload-encrypt/build/libs/apiguard-1.0.0-SNAPSHOT-fat.jar /app.jar
#ENTRYPOINT ["java","-jar","/app.jar"]
