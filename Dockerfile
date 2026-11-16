# syntax=docker/dockerfile:1
# =====================================================================
#  Image multi-stage :
#   1) "build"   : JDK + Maven → compile et produit le jar
#   2) "runtime" : JRE seul + couches Spring Boot extraites (image légère, cache Docker efficace)
# =====================================================================

# ---------- Étape 1 : build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Les dépendances changent rarement : on les télécharge dans une couche séparée (mise en cache).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# ---------- Étape 2 : runtime ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 spring
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER spring

# Valeurs par défaut adaptées à Docker Desktop (Ollama tourne sur la machine hôte).
ENV SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/blueprint \
    SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
