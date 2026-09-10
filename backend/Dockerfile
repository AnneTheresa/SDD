# ---------- Runtime only ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/data /app/logs /app/logs/archived

# Copy ONLY prebuilt jar from Cloud Build
#COPY --from=build /app/target/*.jar app.jar
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]