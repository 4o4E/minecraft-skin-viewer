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
    libgl1 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=jre_source "$JAVA_HOME" "$JAVA_HOME"

ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8" \
    LC_ALL="C" \
    PATH="$JAVA_HOME/bin:$PATH" \
    MCSK_RENDERER_CLASS="top.e404.mcsk.renderer.tavolo.TavoloSkinPngRenderer" \
    MCSK_RENDERER_ID="tavolo-cpu-v1"

COPY enterpoint.sh /enterpoint.sh
RUN chmod +x /enterpoint.sh && \
    useradd --shell /bin/bash -u 1001 -m java

WORKDIR /app
COPY build/libs/http-server-tavolo-linux.jar app.jar

VOLUME /app/skin
VOLUME /app/render-cache

ENTRYPOINT ["/enterpoint.sh"]
