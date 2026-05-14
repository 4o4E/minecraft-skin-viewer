package top.e404.skin.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import top.e404.skin.server.Mojang
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
    }
}
