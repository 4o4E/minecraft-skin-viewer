package top.e404.skin.client

import io.ktor.http.Url
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.Serializable

data class SkinViewerServer(
    val baseUrl: String,
) {
    constructor(baseUrl: Url) : this(baseUrl.toString())
}

sealed class PlayerRef {
    abstract val content: String
    abstract val type: String

    data class Name(override val content: String) : PlayerRef() {
        override val type: String = "name"
    }

    data class Id(override val content: String) : PlayerRef() {
        override val type: String = "id"
    }

    companion object {
        fun name(value: String): PlayerRef = Name(value)
        fun id(value: String): PlayerRef = Id(value)
    }
}

data class RenderPosition(val path: String) {
    companion object {
        val SNEAK: RenderPosition = RenderPosition("sneak")
        val SKIN: RenderPosition = RenderPosition("sk")
        val SKIN_ROTATE: RenderPosition = RenderPosition("dsk")
        val HEAD: RenderPosition = RenderPosition("head")
        val HEAD_ROTATE: RenderPosition = RenderPosition("dhead")
        val HOMO: RenderPosition = RenderPosition("homo")
    }
}

enum class OverlayMode(val queryValue: String) {
    NONE("none"),
    FLAT("flat"),
    THREE_D("3d"),
}

enum class LightingMode(val queryValue: String) {
    AMBIENT("ambient"),
    DIRECTIONAL("directional"),
}

data class RenderColor(val argb: Int) {
    fun toQueryValue(): String = argb.toUInt().toString(16).padStart(8, '0')

    companion object {
        fun argb(value: Int): RenderColor = RenderColor(value)
        fun rgb(value: Int): RenderColor = RenderColor(0xFF000000.toInt() or (value and 0x00FFFFFF))
    }
}

data class RenderSize(
    val width: Int,
    val height: Int,
)

data class RenderVec3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    fun toQueryValue(): String = "$x,$y,$z"
}

data class RenderOptions(
    val backgroundColor: RenderColor? = null,
    val size: RenderSize? = null,
    val target: RenderVec3? = null,
    val yaw: Float? = null,
    val pitch: Float? = null,
    val distance: Float? = null,
    val lightIntensity: Float? = null,
    val lightDirection: RenderVec3? = null,
    val lighting: LightingMode? = null,
    val shadow: Boolean? = null,
    val platform: Boolean? = null,
    val platformTopY: Float? = null,
    val platformThickness: Float? = null,
    val antiAliasingLevel: Int? = null,
    val overlay: OverlayMode? = null,
    val modelYaw: Float? = null,
    val pose: SkinPose? = null,
) {
    fun appendTo(parameters: MutableMap<String, String>) {
        backgroundColor?.let { parameters["bg"] = it.toQueryValue() }
        size?.let {
            parameters["width"] = it.width.toString()
            parameters["height"] = it.height.toString()
        }
        target?.let { parameters["target"] = it.toQueryValue() }
        yaw?.let { parameters["yaw"] = it.toString() }
        pitch?.let { parameters["pitch"] = it.toString() }
        distance?.let { parameters["distance"] = it.toString() }
        lightIntensity?.let { parameters["light"] = it.toString() }
        lightDirection?.let { parameters["lightDirection"] = it.toQueryValue() }
        lighting?.let { parameters["lighting"] = it.queryValue }
        shadow?.let { parameters["shadow"] = it.toString() }
        platform?.let { parameters["platform"] = it.toString() }
        platformTopY?.let { parameters["platformTopY"] = it.toString() }
        platformThickness?.let { parameters["platformThickness"] = it.toString() }
        antiAliasingLevel?.let { parameters["aa"] = it.toString() }
        overlay?.let { parameters["overlay"] = it.queryValue }
        modelYaw?.let { parameters["modelYaw"] = it.toString() }
        pose?.let { parameters["pose"] = it.toJsonString() }
    }
}

data class ModelOptions(
    val slim: Boolean? = null,
    val headScale: Double? = null,
) {
    fun appendTo(parameters: MutableMap<String, String>) {
        slim?.let { parameters["slim"] = it.toString() }
        headScale?.let { parameters["head"] = it.toString() }
    }
}

data class AnimationOptions(
    val frameCount: Int? = null,
    val durationMs: Int? = null,
) {
    fun appendTo(parameters: MutableMap<String, String>) {
        frameCount?.let { parameters["frameCount"] = it.toString() }
        durationMs?.let { parameters["duration"] = it.toString() }
    }
}

data class RenderRequest(
    val player: PlayerRef,
    val position: RenderPosition,
    val render: RenderOptions = RenderOptions(),
    val model: ModelOptions = ModelOptions(),
    val animation: AnimationOptions = AnimationOptions(),
    val extraQueryParameters: Map<String, String> = emptyMap(),
) {
    fun toQueryParameters(): Map<String, String> =
        linkedMapOf<String, String>().also {
            render.appendTo(it)
            model.appendTo(it)
            animation.appendTo(it)
            it.putAll(extraQueryParameters)
        }
}

data class FaceRequest(
    val player: PlayerRef,
    val backgroundColor: RenderColor? = null,
    val scale: Int? = null,
    val margin: Int? = null,
    val extraQueryParameters: Map<String, String> = emptyMap(),
) {
    fun toQueryParameters(): Map<String, String> =
        linkedMapOf<String, String>().also { parameters ->
            backgroundColor?.let { parameters["bg"] = it.toQueryValue() }
            scale?.let { parameters["scale"] = it.toString() }
            margin?.let { parameters["margin"] = it.toString() }
            parameters.putAll(extraQueryParameters)
        }
}

@Serializable
data class SkinData(
    val uuid: String,
    val name: String,
    val slim: Boolean,
    val update: Long,
    val hash: String,
)

enum class BodyPart(val apiName: String) {
    HEAD("head"),
    BODY("body"),
    RIGHT_ARM("rightArm"),
    LEFT_ARM("leftArm"),
    RIGHT_LEG("rightLeg"),
    LEFT_LEG("leftLeg"),
}

data class SkinPose(
    val transforms: Map<BodyPart, List<PoseTransform>>,
) {
    fun toJsonString(): String =
        ClientJson.instance.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                transforms.mapKeys { (part, _) -> part.apiName }
                    .mapValues { (_, transforms) ->
                        JsonArray(transforms.map { it.toJsonObject() })
                    }
            )
        )
}

data class PoseTransform(
    val type: String,
    val x: Float? = null,
    val y: Float? = null,
    val z: Float? = null,
) {
    fun toJsonObject(): JsonObject {
        val fields = linkedMapOf<String, JsonPrimitive>("type" to JsonPrimitive(type))
        x?.let { fields["x"] = JsonPrimitive(it) }
        y?.let { fields["y"] = JsonPrimitive(it) }
        z?.let { fields["z"] = JsonPrimitive(it) }
        return JsonObject(fields)
    }

    companion object {
        fun rotate(x: Float = 0f, y: Float = 0f, z: Float = 0f): PoseTransform =
            PoseTransform(type = "rotate", x = x, y = y, z = z)

        fun scale(x: Float = 1f, y: Float = 1f, z: Float = 1f): PoseTransform =
            PoseTransform(type = "scale", x = x, y = y, z = z)

        fun translate(x: Float = 0f, y: Float = 0f, z: Float = 0f): PoseTransform =
            PoseTransform(type = "translate", x = x, y = y, z = z)
    }
}
