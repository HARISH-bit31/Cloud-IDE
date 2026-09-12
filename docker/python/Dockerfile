# Lightweight isolated execution image for Python 3.12
FROM python:3.12-alpine

# Create a non-root sandbox user
RUN addgroup -S sandbox && adduser -S sandbox -G sandbox

# Prepare workspace
WORKDIR /workspace
RUN chown -R sandbox:sandbox /workspace

USER sandbox

CMD ["sh"]
