# Lightweight isolated execution image for Java 21 LTS
FROM eclipse-temurin:21-jdk-alpine

# Create a non-root sandbox user
RUN addgroup -S sandbox && adduser -S sandbox -G sandbox

# Prepare workspace
WORKDIR /workspace
RUN chown -R sandbox:sandbox /workspace

USER sandbox

CMD ["sh"]
