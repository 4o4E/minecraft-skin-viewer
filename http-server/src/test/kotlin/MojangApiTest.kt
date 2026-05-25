package top.e404.mcsk.test

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import top.e404.mcsk.server.Mojang
import java.util.Base64

class MojangApiTest {
    @Test
    fun `parses player id lookup response`() {
        val json = """[{"id":"22df77dd37b0414b8f1e3c7d2585fc79","name":"404E"}]"""

        assertEquals("22df77dd37b0414b8f1e3c7d2585fc79", Mojang.parseIdByNameResponse(json))
    }

    @Test
    fun `returns null when player id lookup response is empty`() {
        assertNull(Mojang.parseIdByNameResponse("[]"))
    }

    @Test
    fun `parses profile texture response`() {
        val textures = """
            {
              "textures": {
                "SKIN": {
                  "url": "https://textures.minecraft.net/texture/abcdef",
                  "metadata": {
                    "model": "slim"
                  }
                },
                "CAPE": {
                  "url": "https://textures.minecraft.net/texture/capehash"
                }
              }
            }
        """.trimIndent()
        val encodedTextures = Base64.getEncoder().encodeToString(textures.toByteArray())
        val json = """
            {
              "id": "22df77dd37b0414b8f1e3c7d2585fc79",
              "name": "404E",
              "properties": [
                {
                  "name": "textures",
                  "value": "$encodedTextures"
                }
              ]
            }
        """.trimIndent()

        val skin = Mojang.parseProfileResponse(json, update = 123L)

        assertEquals("22df77dd37b0414b8f1e3c7d2585fc79", skin?.uuid)
        assertEquals("404E", skin?.name)
        assertEquals("abcdef", skin?.hash)
        assertEquals("capehash", skin?.capeHash)
        assertEquals(123L, skin?.update)
        assertEquals(true, skin?.slim)
    }

    @Test
    fun `classic model profile is not slim`() {
        val textures = """
            {
              "textures": {
                "SKIN": {
                  "url": "http://textures.minecraft.net/texture/classic"
                }
              }
            }
        """.trimIndent()
        val encodedTextures = Base64.getEncoder().encodeToString(textures.toByteArray())
        val json = """
            {
              "id": "22df77dd37b0414b8f1e3c7d2585fc79",
              "name": "404E",
              "properties": [
                {
                  "name": "textures",
                  "value": "$encodedTextures"
                }
              ]
            }
        """.trimIndent()

        val skin = Mojang.parseProfileResponse(json, update = 123L)

        assertFalse(skin?.slim ?: true)
        assertEquals("classic", skin?.hash)
        assertNull(skin?.capeHash)
    }

    @Test
    fun `live Mojang profile can return cape texture`() = runBlocking {
        assumeTrue(
            System.getProperty("mcsk.mojangLiveTest") == "true" ||
                System.getenv("MCSK_MOJANG_LIVE_TEST") == "true",
            "真实 Mojang 接口测试需要显式启用，避免默认测试依赖外网和账号状态"
        )
        val playerName = System.getProperty("mcsk.mojangCapePlayer")
            ?: System.getenv("MCSK_MOJANG_CAPE_PLAYER")
            ?: "jeb_"

        val uuid = assertNotNull(Mojang.getIdByName(playerName), "Mojang 应返回 $playerName 的 uuid")
        val skin = assertNotNull(Mojang.getById(uuid), "Mojang 应返回 $playerName 的 profile textures")

        assertEquals(playerName.lowercase(), skin.name.lowercase())
        assertNotNull(skin.capeHash, "$playerName 应通过 textures.CAPE 返回披风材质 hash")
    }
}
