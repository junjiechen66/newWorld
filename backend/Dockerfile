FROM maven:3.8.1-openjdk-8-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=builder /app/target/newworld.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
