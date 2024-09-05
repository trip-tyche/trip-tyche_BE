FROM openjdk:17
ARG JAR_FILE=build/libs/memory-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} ./memory-0.0.1-SNAPSHOT.jar
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "./memory-0.0.1-SNAPSHOT.jar"]