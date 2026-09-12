# Lightweight isolated execution image for G++ C++20
FROM alpine:3.20

# Install G++ and musl development tools
RUN apk add --no-cache g++ musl-dev

# Create a non-root sandbox user
RUN addgroup -S sandbox && adduser -S sandbox -G sandbox

# Prepare workspace
WORKDIR /workspace
RUN chown -R sandbox:sandbox /workspace

USER sandbox

CMD ["sh"]
