package top.e404.mcsk.server

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import top.e404.mcsk.server.service.RenderFileCache
import top.e404.mcsk.server.sql.SkinDao
import top.e404.mcsk.server.sql.pojo.SkinData
import java.util.*

object Mojang {
    const val PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/"
    const val ID_URL = "https://api.mojang.com/profiles/minecraft"
    const val TEXTURE_URL = "https://textures.minecraft.net/texture/"

    /**
     * 从MojangApi获取玩家皮肤数据
     *
     * **不会主动存入缓存**
     *
     * @param uuid 玩家uuid
     * @return 皮肤数据
     */
    suspend fun getById(uuid: String): SkinData? {
        val json = client.get("${PROFILE_URL}$uuid").bodyAsText()
        return parseProfileResponse(json)
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
        val ja = client.post(ID_URL) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(buildJsonArray { add(JsonPrimitive(name)) }))
        }.bodyAsText()
        return parseIdByNameResponse(ja)
    }

    internal fun parseProfileResponse(json: String, update: Long = System.currentTimeMillis()): SkinData? {
        if (json.isBlank()) return null
        val jo = json.let { Json.parseToJsonElement(it) }.jsonObject
        val texturesJson = jo["properties"]!!
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
            .jsonObject
        val skinJson = texturesJson["SKIN"]!!.jsonObject
        val slim = skinJson["metadata"]
            ?.jsonObject
            ?.get("model")
            ?.jsonPrimitive
            ?.content == "slim"
        return SkinData(
            jo["id"]!!.jsonPrimitive.content,
            jo["name"]!!.jsonPrimitive.content,
            slim,
            update,
            skinJson["url"]!!.jsonPrimitive.content
                .removePrefix("http://textures.minecraft.net/texture/")
                .removePrefix("https://textures.minecraft.net/texture/"),
            texturesJson["CAPE"]?.jsonObject
                ?.get("url")
                ?.jsonPrimitive
                ?.content
                ?.removePrefix("http://textures.minecraft.net/texture/")
                ?.removePrefix("https://textures.minecraft.net/texture/")
        )
    }

    internal fun parseIdByNameResponse(json: String): String? {
        val ja = json.let { Json.parseToJsonElement(it).jsonArray }
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
            proxy = it.proxy
        }
    }
}

object Skin {
    suspend fun getByName(name: String): SkinData? {
        FixtureSkin.byName(name)?.let { return it }
        val exists = SkinDao.getByName(name)
        if (exists != null && !exists.isExpired()) return exists
        val id = Mojang.getIdByName(name) ?: return null
        return Mojang.getById(id)?.also { data -> saveRefreshedSkin(exists, data) }
    }

    suspend fun getById(id: String): SkinData? {
        FixtureSkin.byId(id)?.let { return it }
        val exists = SkinDao.getById(id)
        if (exists != null && !exists.isExpired()) return exists
        return Mojang.getById(id)?.also { data -> saveRefreshedSkin(exists, data) }
    }

    suspend fun refreshByName(name: String): Boolean {
        if (FixtureSkin.refreshByName(name)) return true
        val old = SkinDao.getByName(name)
        val id = Mojang.getIdByName(name) ?: return false
        return Mojang.getById(id)?.also { data ->
            clearSkinFilesAndRenderCache(old, data, force = true)
            SkinDao.add(data)
        } != null
    }

    suspend fun refreshById(id: String): Boolean {
        if (FixtureSkin.refreshById(id)) return true
        val old = SkinDao.getById(id)
        return Mojang.getById(id)?.also { data ->
            clearSkinFilesAndRenderCache(old, data, force = true)
            SkinDao.add(data)
        } != null
    }

    private suspend fun saveRefreshedSkin(old: SkinData?, data: SkinData) {
        clearSkinFilesAndRenderCache(old, data, force = false)
        SkinDao.add(data)
    }

    private suspend fun clearSkinFilesAndRenderCache(old: SkinData?, data: SkinData, force: Boolean) {
        val hashChanged = old?.hash != null && old.hash != data.hash
        val capeHashChanged = old?.capeHash != data.capeHash
        val uuidChanged = old?.uuid != null && old.uuid != data.uuid
        if (force || hashChanged || capeHashChanged || uuidChanged) {
            old?.uuid?.let { RenderFileCache.clearUuid(it) }
            if (old?.uuid != data.uuid) RenderFileCache.clearUuid(data.uuid)
            withContext(Dispatchers.IO) {
                old?.skinFile?.delete()
                old?.capeFile?.delete()
                data.skinFile.delete()
                data.capeFile.delete()
            }
        }
    }

    private fun SkinData.isExpired(): Boolean {
        val timeoutMillis = ConfigManager.config.timeout * 1000L
        return System.currentTimeMillis() - update > timeoutMillis
    }
}
