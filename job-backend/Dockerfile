# Stage 1: Build ứng dụng bằng Maven với Java 17 (Temurin)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Sao chép file cấu hình và tải trước dependency để tận dụng cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Sao chép code và build file JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng với JRE 17 (Temurin) cực nhẹ cho Production
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Sao chép file JAR từ stage build
COPY --from=build /app/target/*.jar app.jar

# Cấu hình Port cho Render
ENV PORT=8080
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["sh", "-c", "java -Xmx400m -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar"]