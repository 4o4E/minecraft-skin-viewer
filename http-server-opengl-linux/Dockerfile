FROM eclipse-temurin:11-jre-jammy AS jre_source
FROM ubuntu:24.04

ENV TZ="Asia/Shanghai"

RUN sed -i s/archive.ubuntu.com/mirrors.tuna.tsinghua.edu.cn/i /etc/apt/sources.list.d/ubuntu.sources && \
    sed -i s/security.ubuntu.com/mirrors.tuna.tsinghua.edu.cn/i /etc/apt/sources.list.d/ubuntu.sources && \
    DEBIAN_FRONTEND=noninteractive \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    gosu \
    ca-certificates \
    fontconfig libfreetype6 \
    xvfb \
    libgl1 \
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
ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8" \
    LC_ALL="C" \
    PATH="$JAVA_HOME/bin:$PATH" \
    DISPLAY=":99" \
    LIBGL_ALWAYS_SOFTWARE="1" \
    SKIN_RENDERER_CLASS="top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer" \
    SKIN_RENDERER_ID="opengl-lwjgl-fbo-xvfb"
COPY --from=jre_source "$JAVA_HOME" "$JAVA_HOME"

COPY enterpoint.sh /enterpoint.sh
RUN chmod +x /enterpoint.sh && \
    useradd --shell /bin/bash -u 1001 -m java

WORKDIR /app
COPY build/libs/http-server-opengl-linux.jar app.jar

VOLUME /app/skin
VOLUME /app/render-cache

ENTRYPOINT ["/enterpoint.sh"]
