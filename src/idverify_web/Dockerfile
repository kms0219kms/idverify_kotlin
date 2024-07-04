FROM openjdk:19-jdk-alpine
LABEL org.opencontainers.image.source https://github.com/NGuard-Security/hiplay_idverify

# Initial Setup
WORKDIR /usr/app
RUN apt update && apt install curl tar xz-utils git -y
RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# Create Spring Group
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy Jar File
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

CMD ["java","-jar","/app.jar"]
