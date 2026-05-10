# STAGE 1: FRONTEND
# Vite 6+ requer Node.js 20+ ou 22+
FROM node:20-alpine AS frontend-builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# STAGE 2: BACKEND
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline
COPY backend/src ./src
# Copia o build do frontend para os recursos estáticos do Spring
RUN mkdir -p src/main/resources/static
COPY --from=frontend-builder /app/dist/ src/main/resources/static/
RUN mvn clean package -DskipTests

# STAGE 3: RUNTIME
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/*.jar app.jar
# Render usa a variável PORT, o Spring vai escutar nela
ENV PORT=8081
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
