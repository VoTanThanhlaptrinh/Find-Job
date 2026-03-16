# Stage 1: Build ứng dụng bằng Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Sao chép file pom.xml và download các dependencies (để tận dụng cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Sao chép toàn bộ code và build file JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng với JRE nhẹ
FROM openjdk:17-jdk-slim
WORKDIR /app

# Sao chép file JAR đã build từ stage trước
COPY --from=build /app/target/*.jar app.jar

# Cấu hình biến môi trường cho Port (Render sẽ cấp phát biến $PORT)
ENV PORT=8080
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]