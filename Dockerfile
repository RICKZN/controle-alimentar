FROM maven:3.9-eclipse-temurin-17

RUN apt-get update && apt-get install -y curl
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
RUN apt-get install -y nodejs

WORKDIR /app

COPY frontend/package*.json ./frontend/
RUN cd frontend && npm install
COPY frontend/ ./frontend/
ENV NODE_OPTIONS=--max-old-space-size=512
RUN cd frontend && npm run build

COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src
RUN mkdir -p backend/src/main/resources/static
RUN cp -r frontend/dist/* backend/src/main/resources/static/

RUN cd backend && MAVEN_OPTS="-Xmx512m -Xms256m" mvn clean package -DskipTests

EXPOSE 8081
ENV PORT=8081
CMD ["java", "-jar", "backend/target/controle-alimentar-0.0.1-SNAPSHOT.jar", "--server.port=${PORT}"]
