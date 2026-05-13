#!/bin/bash
set -e

cat > db.properties << EOF
jdbcUrl=$DB_URL
username=$DB_USERNAME
password=$DB_PASSWORD
EOF

cat > config.yml << EOF
host: "${SERVER_HOST:-0.0.0.0}"
port: ${SERVER_PORT:-80}
EOF

if [ -n "$PROXY_TYPE" ]; then
  cat >> config.yml << EOF
proxy:
  type: $PROXY_TYPE
  host: $PROXY_HOST
  port: $PROXY_PORT
EOF
fi

chown -R java /app

if [ -z "$START_CMD" ]; then
  START_CMD="java -jar app.jar"
fi

exec gosu java bash -lc "$START_CMD"
