FROM openjdk:12-jdk-alpine
VOLUME /tmp
# ARG JAR_FILE
# COPY ${JAR_FILE} app.jar
COPY build/libs/apiguard-1.0.0-SNAPSHOT-fat.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
