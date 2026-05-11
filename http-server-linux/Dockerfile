FROM openjdk:11-jre-slim AS jre_source
# ubuntu 作为基础镜像。
FROM ubuntu:24.04

# 时区
ENV TZ="Asia/Shanghai"

# 安装依赖
RUN sed -i s/archive.ubuntu.com/mirrors.tuna.tsinghua.edu.cn/i /etc/apt/sources.list.d/ubuntu.sources && \
    sed -i s/security.ubuntu.com/mirrors.tuna.tsinghua.edu.cn/i /etc/apt/sources.list.d/ubuntu.sources && \
    DEBIAN_FRONTEND=noninteractive \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    gosu \
    ca-certificates \
    fontconfig libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/local/openjdk-11
ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8" \
    LC_ALL="C" \
    PATH="$JAVA_HOME/bin:$PATH"
COPY --from=jre_source "$JAVA_HOME" "$JAVA_HOME"

COPY enterpoint.sh /enterpoint.sh
RUN chmod +x /enterpoint.sh && \
    useradd --shell /bin/bash -u 1001 -m java

WORKDIR /app
COPY build/libs/http-server-linux.jar app.jar

# 皮肤缓存
VOLUME /app/skin
# 渲染结果缓存
VOLUME /app/render-cache

# 容器启动时执行的命令
ENTRYPOINT ["/enterpoint.sh"]
