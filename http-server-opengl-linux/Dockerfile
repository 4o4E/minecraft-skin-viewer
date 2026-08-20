FROM eclipse-temurin:11-jre-jammy AS jre_source
FROM ubuntu:24.04

ENV TZ="Asia/Shanghai"

ARG UBUNTU_MIRROR=""

RUN if [ -n "$UBUNTU_MIRROR" ]; then \
        sed -Ei "s#https?://(archive|security).ubuntu.com#$UBUNTU_MIRROR#g" /etc/apt/sources.list.d/ubuntu.sources; \
    fi && \
    DEBIAN_FRONTEND=noninteractive \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    gosu \
    ca-certificates \
    fontconfig libfreetype6 \
    xvfb \
    libegl1 \
    libgl1 \
    libglvnd0 \
    libopengl0 \
    libglx-mesa0 \
    libgl1-mesa-dri \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxi6 \
    libxrandr2 \
    libxcursor1 \
    libxinerama1 \
    libxxf86vm1 \
    mesa-utils \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=jre_source "$JAVA_HOME" "$JAVA_HOME"

ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8" \
    LC_ALL="C" \
    PATH="$JAVA_HOME/bin:$PATH" \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true" \
    MCSK_GL_BACKEND="xvfb" \
    MCSK_RENDERER_CLASS="top.e404.mcsk.renderer.opengl.OpenGlSkinPngRenderer"

COPY nvidia/10_nvidia.json /usr/share/glvnd/egl_vendor.d/10_nvidia.json
COPY enterpoint.sh /enterpoint.sh
RUN chmod 0644 /usr/share/glvnd/egl_vendor.d/10_nvidia.json && \
    chmod +x /enterpoint.sh && \
    useradd --shell /bin/bash -u 1001 -m java

WORKDIR /app
COPY build/libs/http-server-opengl-linux.jar app.jar

VOLUME /app/skin
VOLUME /app/render-cache

ENTRYPOINT ["/enterpoint.sh"]
