# STAGE 1: FRONTEND
FROM node:20-bullseye AS frontend-builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# STAGE 2: BACKEND
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline
COPY backend/src ./src
RUN mkdir -p src/main/resources/static
COPY --from=frontend-builder /app/dist/ src/main/resources/static/
RUN mvn clean package -DskipTests

# STAGE 3: RUNTIME
FROM eclipse-temurin:17-jre-bullseye
WORKDIR /app
COPY --from=backend-builder /app/target/*.jar app.jar
ENV PORT=8081
EXPOSE 8081
CMD ["java", "-jar", "-Dserver.port=${PORT}", "app.jar"]
