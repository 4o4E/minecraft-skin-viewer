package top.e404.skin.server

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import top.e404.skin.server.sql.pojo.SkinData
import top.e404.skin.server.sql.useSkinMapper
import java.net.Proxy
import java.util.*

object Mojang {
    private const val profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/"
    private const val idUrl = "https://api.mojang.com/profiles/minecraft"

    /**
     * 从MojangApi获取玩家皮肤数据
     *
     * **不会主动存入缓存**
     *
     * @param uuid 玩家uuid
     * @return 皮肤数据
     */
    suspend fun getById(uuid: String): SkinData? {
        val json = client.get("${profileUrl}$uuid").bodyAsText()
        if (json.isBlank()) return null
        val jo = json.let { Json.parseToJsonElement(it) }.jsonObject
        val skinJson = jo["properties"]!!
            .jsonArray
            .asSequence()
            .map { it.jsonObject }
            .first { it["name"]!!.jsonPrimitive.content == "textures" }
            .jsonObject["value"]!!
            .jsonPrimitive
            .content
            .let { Base64.getDecoder().decode(it).toString(Charsets.UTF_8) }
            .let { Json.parseToJsonElement(it) }
            .jsonObject["textures"]!!
            .jsonObject["SKIN"]!!
            .jsonObject
        val slim = skinJson["metadata"]
            ?.jsonObject
            ?.get("model")
            ?.jsonPrimitive
            ?.content == "slim"
        return SkinData(
            jo["id"]!!.jsonPrimitive.content,
            jo["name"]!!.jsonPrimitive.content,
            slim,
            System.currentTimeMillis(),
            skinJson["url"]!!.jsonPrimitive.content.removePrefix("http://textures.minecraft.net/texture/")
        )
    }

    /**
     * 从MojangApi获取玩家id
     *
     * **不会主动存入缓存**
     *
     * @param name 玩家名
     * @return 玩家uuid, 若不存在此玩家则返回null
     */
    suspend fun getIdByName(name: String): String? {
        val ja = client.post(idUrl) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(buildJsonArray { add(JsonPrimitive(name)) }))
        }.bodyAsText().let { Json.parseToJsonElement(it).jsonArray }
        if (ja.isEmpty()) return null
        val jo = ja.first().jsonObject
        return jo["id"]!!.jsonPrimitive.content
    }
}

val client = HttpClient(OkHttp) {
    engine {
        config {
            followRedirects(true)
        }
        ConfigManager.config.proxy?.let {
            proxy = Proxy(Proxy.Type.HTTP, it.socketAddress)
        }
    }
}

object Skin {
    suspend fun getByName(name: String): SkinData? {
        val exists = useSkinMapper { it.getByName(name) }
        if (exists != null && System.currentTimeMillis() - exists.update > ConfigManager.config.timeout) return exists
        val id = Mojang.getIdByName(name) ?: return null
        return Mojang.getById(id)?.also { data -> useSkinMapper { it.add(data) } }
    }

    suspend fun getById(id: String): SkinData? {
        val exists = useSkinMapper { it.getById(id) }
        if (exists != null && System.currentTimeMillis() - exists.update > ConfigManager.config.timeout) return exists
        return Mojang.getById(id)?.also { data -> useSkinMapper { it.add(data) } }
    }

    suspend fun refreshByName(name: String): Boolean {
        val id = Mojang.getIdByName(name) ?: return false
        return Mojang.getById(id)?.also { data ->
            withContext(Dispatchers.IO) { data.skinFile.delete() }
            useSkinMapper { it.add(data) }
        } != null
    }

    suspend fun refreshById(id: String) = Mojang.getById(id)?.also { data ->
        withContext(Dispatchers.IO) { data.skinFile.delete() }
        useSkinMapper { it.add(data) }
    } != null
}
