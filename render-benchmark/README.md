# render-benchmark

This module compares Minecraft skin PNG rendering performance.

Renderer implementations are imported as normal dependencies:

- `:renderer-tavolo` provides `top.e404.mcsk.renderer.tavolo.TavoloSkinPngRenderer`.
- `:renderer-opengl` provides `top.e404.mcsk.renderer.opengl.OpenGlSkinPngRenderer`.
- `:core` owns `SkinPngRenderer`, `SkinRenderRequest`, neutral geometry, model definitions, and UV mapping.

Run:

```bash
./gradlew :render-benchmark:renderImageBenchmark
```

Output:

```text
render-benchmark/build/reports/render-image-benchmark/summary.csv
render-benchmark/build/reports/render-image-benchmark/summary.md
render-benchmark/build/reports/render-image-benchmark/images/*.png
```

Docker:

```bash
./gradlew :render-benchmark:shadowJar
docker build -f render-benchmark/Dockerfile -t minecraft-skin-render-benchmark .
docker run --rm minecraft-skin-render-benchmark
```
