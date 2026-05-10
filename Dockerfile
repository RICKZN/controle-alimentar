# Estágio 1: Build do Frontend (Vue)
FROM node:18-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Estágio 2: Build do Backend (Spring Boot)
FROM maven:3.8.5-eclipse-temurin-17 AS backend-build
WORKDIR /app/backend
COPY backend/pom.xml .
RUN mvn dependency:go-offline
COPY backend/src ./src
RUN mkdir -p src/main/resources/static
COPY --from=frontend-build /app/frontend/dist/ src/main/resources/static/
RUN mvn clean package -DskipTests

# Estágio 3: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/target/controle-alimentar-0.0.1-SNAPSHOT.jar app.jar
# Render usa a variável de ambiente PORT, vamos configurar o Spring para escutar nela
ENV PORT=8081
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
