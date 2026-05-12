FROM 404e/minecraft-skin-viewer:1.5.0

ENV JAVA_HOME=/usr/local/openjdk-11
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

COPY render-benchmark/build/libs/render-benchmark.jar /app/render-benchmark.jar

ENTRYPOINT []
CMD ["sh", "-c", "Xvfb :99 -screen 0 1280x1024x24 +extension GLX +render -noreset -nolisten tcp >/tmp/xvfb.log 2>&1 & XVFB_PID=$!; trap 'kill $XVFB_PID' EXIT; export DISPLAY=:99; sleep 1; java -Dfile.encoding=UTF-8 -Dprism.order=es2 -Dprism.forceGPU=true -jar /app/render-benchmark.jar"]
