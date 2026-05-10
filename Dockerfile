FROM maven:3.8.4-openjdk-17 AS build
COPY backend /app/backend
COPY frontend /app/frontend
WORKDIR /app/frontend
RUN npm install && npm run build
RUN mkdir -p /app/backend/src/main/resources/static && cp -r dist/* /app/backend/src/main/resources/static/
WORKDIR /app/backend
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
COPY --from=build /app/backend/target/controle-alimentar-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
