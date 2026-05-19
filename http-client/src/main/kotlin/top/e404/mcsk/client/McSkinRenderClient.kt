package top.e404.mcsk.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString

class McSkinRenderClient private constructor(
    private val server: McSkinRenderServer,
    private val httpClient: HttpClient,
    private val closeHttpClient: Boolean,
) : AutoCloseable {
    constructor(server: McSkinRenderServer) : this(server, defaultHttpClient(), closeHttpClient = true)

    constructor(server: McSkinRenderServer, httpClient: HttpClient) : this(server, httpClient, closeHttpClient = false)

    constructor(baseUrl: String) : this(McSkinRenderServer(baseUrl))

    constructor(baseUrl: String, httpClient: HttpClient) : this(McSkinRenderServer(baseUrl), httpClient)

    constructor(baseUrl: Url) : this(McSkinRenderServer(baseUrl))

    constructor(baseUrl: Url, httpClient: HttpClient) : this(McSkinRenderServer(baseUrl), httpClient)

    suspend fun render(request: RenderRequest): ByteArray =
        getBytes(
            pathSegments = listOf("render", request.player.type, request.player.content, request.position.path),
            queryParameters = request.toQueryParameters()
        )

    suspend fun renderSkin(
        player: PlayerRef,
        render: RenderOptions = RenderOptions(),
        model: ModelOptions = ModelOptions(),
    ): ByteArray =
        render(RenderRequest(player, RenderPosition.SKIN, render, model))

    suspend fun renderSkinRotate(
        player: PlayerRef,
        render: RenderOptions = RenderOptions(),
        model: ModelOptions = ModelOptions(),
        animation: AnimationOptions = AnimationOptions(),
    ): ByteArray =
        render(RenderRequest(player, RenderPosition.SKIN_ROTATE, render, model, animation))

    suspend fun renderSneak(
        player: PlayerRef,
        render: RenderOptions = RenderOptions(),
        model: ModelOptions = ModelOptions(),
        animation: AnimationOptions = AnimationOptions(),
    ): ByteArray =
        render(RenderRequest(player, RenderPosition.SNEAK, render, model, animation))

    suspend fun renderHead(
        player: PlayerRef,
        render: RenderOptions = RenderOptions(),
    ): ByteArray =
        render(RenderRequest(player, RenderPosition.HEAD, render))

    suspend fun renderHeadRotate(
        player: PlayerRef,
        render: RenderOptions = RenderOptions(),
        animation: AnimationOptions = AnimationOptions(),
    ): ByteArray =
        render(RenderRequest(player, RenderPosition.HEAD_ROTATE, render, animation = animation))

    suspend fun renderHomo(
        player: PlayerRef,
        render: RenderOptions = RenderOptions(),
        model: ModelOptions = ModelOptions(),
    ): ByteArray =
        render(RenderRequest(player, RenderPosition.HOMO, render, model))

    suspend fun face(request: FaceRequest): ByteArray =
        getBytes(
            pathSegments = listOf("face", request.player.type, request.player.content),
            queryParameters = request.toQueryParameters()
        )

    suspend fun face(
        player: PlayerRef,
        backgroundColor: RenderColor? = null,
        scale: Int? = null,
        margin: Int? = null,
    ): ByteArray =
        face(FaceRequest(player, backgroundColor, scale, margin))

    suspend fun refresh(player: PlayerRef): Boolean {
        val response = httpClient.get(buildUrl(listOf("refresh", player.type, player.content)))
        return when (response.status) {
            HttpStatusCode.OK -> true
            HttpStatusCode.NotFound -> false
            else -> throw response.toException()
        }
    }

    suspend fun data(player: PlayerRef): SkinData {
        val response = httpClient.get(buildUrl(listOf("data", player.type, player.content)))
        if (!response.status.isSuccess()) throw response.toException()
        return ClientJson.instance.decodeFromString(response.bodyAsText())
    }

    override fun close() {
        // 只关闭本模块创建的默认客户端，避免影响下游复用的 HttpClient。
        if (closeHttpClient) httpClient.close()
    }

    private suspend fun getBytes(
        pathSegments: List<String>,
        queryParameters: Map<String, String> = emptyMap(),
    ): ByteArray {
        val response = httpClient.get(buildUrl(pathSegments, queryParameters))
        if (!response.status.isSuccess()) throw response.toException()
        return response.readBytes()
    }

    private fun buildUrl(
        pathSegments: List<String>,
        queryParameters: Map<String, String> = emptyMap(),
    ): String =
        URLBuilder(Url(server.baseUrl)).apply {
            appendPathSegments(pathSegments)
            queryParameters.forEach { (name, value) ->
                parameters.append(name, value)
            }
        }.buildString()

    private suspend fun HttpResponse.toException(): McSkinRenderHttpException =
        McSkinRenderHttpException(status, bodyAsText())

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient(OkHttp)
    }
}

class McSkinRenderHttpException(
    val status: HttpStatusCode,
    val responseText: String,
) : RuntimeException("Skin render request failed: ${status.value} ${status.description}; $responseText")
