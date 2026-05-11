#!/bin/bash

echo 创建db.properties
cat > db.properties << EOF
jdbcUrl=$DB_URL
username=$DB_USERNAME
password=$DB_PASSWORD
EOF

echo 创建config.yml
cat > config.yml << EOF
host: "$SERVER_HOST"
port: $SERVER_PORT
EOF

if [ -n "$PROXY_TYPE" ]; then
  cat >> config.yml << EOF
proxy:
  type: $PROXY_TYPE
  host: $PROXY_HOST
  port: $PROXY_PORT
EOF
fi

# 修正文件归属
chown -R java /app

if [ ! -n "$START_CMD" ]; then
  START_CMD="java -jar app.jar"
fi
gosu java $START_CMD
