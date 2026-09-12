# Lightweight isolated execution image for GCC C23 / C17
FROM alpine:3.20

# Install GCC and musl development tools
RUN apk add --no-cache gcc musl-dev

# Create a non-root sandbox user
RUN addgroup -S sandbox && adduser -S sandbox -G sandbox

# Prepare workspace
WORKDIR /workspace
RUN chown -R sandbox:sandbox /workspace

USER sandbox

CMD ["sh"]
