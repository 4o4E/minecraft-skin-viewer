package top.e404.mcsk.renderer.opengl

/**
 * 供容器部署前验证 OpenGL 后端、驱动厂商和显卡型号，不启动 HTTP 服务或访问数据库。
 */
object OpenGlBackendProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        OpenGlSkinPngRenderer().also { renderer ->
            renderer.startup()
            renderer.close()
        }
    }
}
