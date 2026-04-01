FROM openjdk:17-jdk-slim

WORKDIR /app

COPY Server.java .

RUN javac Server.java

EXPOSE 8080

CMD ["java", "Server"]