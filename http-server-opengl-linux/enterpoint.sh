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

case "${MCSK_GL_BACKEND:-xvfb}" in
  xvfb|glfw)
    export DISPLAY="${DISPLAY:-:99}"
    export LIBGL_ALWAYS_SOFTWARE="${LIBGL_ALWAYS_SOFTWARE:-1}"
    export MCSK_RENDERER_ID="${MCSK_RENDERER_ID:-opengl-lwjgl-fbo-xvfb}"
    rm -f /tmp/.X99-lock
    Xvfb "$DISPLAY" -screen 0 "${XVFB_SCREEN:-1024x768x24}" +extension GLX +render -noreset -ac &
    XVFB_PID=$!
    trap 'kill "$XVFB_PID" 2>/dev/null || true' EXIT
    for i in {1..30}; do
      if gosu java glxinfo -B >/dev/null 2>&1; then
        break
      fi
      if [ "$i" -eq 30 ]; then
        echo "Xvfb/GLX did not become ready" >&2
        exit 1
      fi
      sleep 0.2
    done
    ;;
  egl|nvidia)
    unset DISPLAY
    unset LIBGL_ALWAYS_SOFTWARE
    export MCSK_RENDERER_ID="${MCSK_RENDERER_ID:-opengl-lwjgl-fbo-egl}"
    ;;
  *)
    echo "Unsupported MCSK_GL_BACKEND: $MCSK_GL_BACKEND" >&2
    exit 1
    ;;
esac

if [ -z "$START_CMD" ]; then
  START_CMD="java -jar app.jar"
fi

exec gosu java bash -lc "$START_CMD"
