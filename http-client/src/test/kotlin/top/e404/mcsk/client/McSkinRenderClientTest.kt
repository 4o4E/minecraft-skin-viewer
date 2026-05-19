package top.e404.mcsk.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class McSkinRenderClientTest {
    @Test
    fun `render skin builds typed render request query`() = runBlocking {
        var requestedUrl = ""
        val client = testClient { request ->
            requestedUrl = request.url.toString()
            respond(
                content = ByteArray(4) { it.toByte() },
                headers = headersOf(HttpHeaders.ContentType, "image/png")
            )
        }

        val bytes = client.renderSkin(
            player = PlayerRef.name("404E"),
            render = RenderOptions(
                backgroundColor = RenderColor.rgb(0x112233),
                lightIntensity = 0.7f,
                lightDirection = RenderVec3(0.5f, 0.9f, 0.35f),
                shadow = true,
                overlay = OverlayMode.THREE_D,
                platform = true,
            ),
            model = ModelOptions(slim = true, headScale = 1.5)
        )

        assertEquals(listOf<Byte>(0, 1, 2, 3), bytes.toList())
        assertTrue(requestedUrl.startsWith("http://render.test/api/render/name/404E/sk?"))
        assertTrue(requestedUrl.contains("bg=ff112233"))
        assertTrue(requestedUrl.contains("light=0.7"))
        assertTrue(requestedUrl.contains("lightDirection=0.5%2C0.9%2C0.35"))
        assertTrue(requestedUrl.contains("shadow=true"))
        assertTrue(requestedUrl.contains("overlay=3d"))
        assertTrue(requestedUrl.contains("platform=true"))
        assertTrue(requestedUrl.contains("slim=true"))
        assertTrue(requestedUrl.contains("head=1.5"))
    }

    @Test
    fun `render request supports pose parameters`() = runBlocking {
        var requestedUrl = ""
        val client = testClient { request ->
            requestedUrl = request.url.toString()
            respond(ByteArray(0))
        }

        client.renderSneak(
            player = PlayerRef.id("uuid"),
            render = RenderOptions(
                pose = SkinPose(
                    mapOf(
                        BodyPart.BODY to listOf(PoseTransform.rotate(x = 30f)),
                        BodyPart.HEAD to listOf(PoseTransform.translate(y = -1f, z = 2f))
                    )
                )
            )
        )

        assertTrue(requestedUrl.startsWith("http://render.test/api/render/id/uuid/sneak?"))
        assertTrue(requestedUrl.contains("pose="))
        assertTrue(requestedUrl.contains("%22body%22"))
        assertTrue(requestedUrl.contains("%22rotate%22"))
        assertTrue(requestedUrl.contains("%22head%22"))
    }

    @Test
    fun `render request supports custom position and ktor url`() = runBlocking {
        var requestedUrl = ""
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    requestedUrl = request.url.toString()
                    respond(ByteArray(0))
                }
            }
        }
        val client = McSkinRenderClient(Url("http://render.test/api"), httpClient)

        client.render(RenderRequest(PlayerRef.name("404E"), RenderPosition("new-mode")))

        assertEquals("http://render.test/api/render/name/404E/new-mode", requestedUrl)
    }

    @Test
    fun `render request appends extra query parameters`() = runBlocking {
        var requestedUrl = ""
        val client = testClient { request ->
            requestedUrl = request.url.toString()
            respond(ByteArray(0))
        }

        client.render(
            RenderRequest(
                player = PlayerRef.name("404E"),
                position = RenderPosition.SKIN,
                extraQueryParameters = linkedMapOf(
                    "targetX" to "0",
                    "targetY" to "10",
                    "targetZ" to "0"
                )
            )
        )

        assertTrue(requestedUrl.startsWith("http://render.test/api/render/name/404E/sk?"))
        assertTrue(requestedUrl.contains("targetX=0"))
        assertTrue(requestedUrl.contains("targetY=10"))
        assertTrue(requestedUrl.contains("targetZ=0"))
    }

    @Test
    fun `face request appends extra query parameters`() = runBlocking {
        var requestedUrl = ""
        val client = testClient { request ->
            requestedUrl = request.url.toString()
            respond(ByteArray(0))
        }

        client.face(
            FaceRequest(
                player = PlayerRef.name("404E"),
                extraQueryParameters = linkedMapOf(
                    "bg" to "#ffff",
                    "scale" to "5",
                    "margin" to "40"
                )
            )
        )

        assertTrue(requestedUrl.startsWith("http://render.test/api/face/name/404E?"))
        assertTrue(requestedUrl.contains("bg=%23ffff"))
        assertTrue(requestedUrl.contains("scale=5"))
        assertTrue(requestedUrl.contains("margin=40"))
    }

    @Test
    fun `data parses skin data response`() = runBlocking {
        val client = testClient {
            respond(
                content = """{"uuid":"u","name":"n","slim":true,"update":123,"hash":"h"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val data = client.data(PlayerRef.name("404E"))

        assertEquals(SkinData("u", "n", true, 123, "h"), data)
    }

    @Test
    fun `refresh returns false on not found`() = runBlocking {
        val client = testClient {
            respond("", status = HttpStatusCode.NotFound)
        }

        assertEquals(false, client.refresh(PlayerRef.name("missing")))
    }

    @Test
    fun `http errors expose status and response text`() = runBlocking {
        val client = testClient {
            respond("bad request", status = HttpStatusCode.BadRequest)
        }

        val error = assertFailsWith<McSkinRenderHttpException> {
            client.renderHead(PlayerRef.name("bad"))
        }

        assertEquals(HttpStatusCode.BadRequest, error.status)
        assertEquals("bad request", error.responseText)
    }

    private fun testClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): McSkinRenderClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler(handler)
            }
        }
        return McSkinRenderClient("http://render.test/api", httpClient)
    }
}
