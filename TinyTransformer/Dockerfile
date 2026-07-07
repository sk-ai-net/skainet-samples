# =========== BUILD STAGE ===========
FROM gradle:9.0-jdk17 AS build


WORKDIR /app

# Copy the Gradle files and source
COPY . .

# Build the web distribution
RUN ./gradlew --info :composeApp:wasmJsBrowserProductionWebpack --no-daemon

# =========== RUNTIME STAGE ===========
FROM nginx:latest

# Copy the resulting distribution from the build stage
COPY --from=build /app/composeApp/build/kotlin-webpack/wasmJs/productionExecutable/ /usr/share/nginx/html/
COPY --from=build /app/composeApp/build/processedResources/wasmJs/main/ /usr/share/nginx/html/

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]