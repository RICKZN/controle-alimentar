# Use a single stage with full tools for maximum reliability
FROM maven:3.9-eclipse-temurin-17

# 1. Install Node.js directly (needed for frontend)
RUN apt-get update && apt-get install -y curl
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
RUN apt-get install -y nodejs

WORKDIR /app

# 2. Build Frontend
COPY frontend/package*.json ./frontend/
RUN cd frontend && npm install
COPY frontend/ ./frontend/
RUN cd frontend && npm run build

# 3. Build Backend
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src
RUN mkdir -p backend/src/main/resources/static
RUN cp -r frontend/dist/* backend/src/main/resources/static/

RUN cd backend && mvn clean package -DskipTests

# 4. Runtime
EXPOSE 8081
ENV PORT=8081
CMD ["java", "-jar", "backend/target/controle-alimentar-0.0.1-SNAPSHOT.jar", "--server.port=${PORT}"]
