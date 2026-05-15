package top.e404.skin.server.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.e404.skin.server.ConfigManager
import top.e404.skin.server.FixtureSkin
import top.e404.skin.server.sql.RenderCacheDao
import top.e404.skin.server.sql.RenderCacheRecord
import top.e404.skin.server.sql.pojo.SkinData
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object RenderFileCache {
    private const val CACHE_VERSION = 7
    private val renderLocks = ConcurrentHashMap<CacheKey, RenderLock>()

    fun paramsMd5(values: Map<String, Any?>): String {
        val normalized = buildString {
            appendLine("cacheVersion=$CACHE_VERSION")
            values.toSortedMap().forEach { (key, value) ->
                append(key)
                append('=')
                appendLine(value.cacheDigestValue())
            }
        }
        return normalized.md5()
    }

    private fun Any?.cacheDigestValue(): String = when (this) {
        null -> "null:"
        is Boolean -> "boolean:$this"
        is Number -> "number:$this"
        else -> "string:$this"
    }

    suspend fun getOrRender(
        skin: SkinData,
        paramsMd5: String,
        ext: String,
        render: suspend () -> ByteArray,
    ): ByteArray {
        if (FixtureSkin.enabled) return render()
        if (!ConfigManager.config.renderCache.enabled) return render()

        return withRenderLock(CacheKey(skin.uuid, paramsMd5, ext)) {
            getOrRenderLocked(skin, paramsMd5, ext, render)
        }
    }

    private suspend fun <T> withRenderLock(key: CacheKey, block: suspend () -> T): T {
        val lock = renderLocks.compute(key) { _, current ->
            (current ?: RenderLock()).also { it.references++ }
        }!!
        try {
            return lock.mutex.withLock { block() }
        } finally {
            renderLocks.computeIfPresent(key) { _, current ->
                current.references--
                if (current.references == 0) null else current
            }
        }
    }

    private suspend fun getOrRenderLocked(
        skin: SkinData,
        paramsMd5: String,
        ext: String,
        render: suspend () -> ByteArray,
    ): ByteArray {
        val file = cacheFile(skin.uuid, paramsMd5, ext)
        val record = RenderCacheDao.find(skin.uuid, paramsMd5, ext)
        val now = System.currentTimeMillis()
        if (record != null && record.skinHash == skin.hash && file.isFile) {
            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
            RenderCacheDao.touch(skin.uuid, paramsMd5, ext, now)
            return bytes
        }

        if (record != null) {
            RenderCacheDao.delete(skin.uuid, paramsMd5, ext)
            withContext(Dispatchers.IO) { file.delete() }
        }

        val bytes = render()
        writeAtomically(file, bytes)
        try {
            RenderCacheDao.upsert(
                RenderCacheRecord(
                    uuid = skin.uuid,
                    paramsMd5 = paramsMd5,
                    ext = ext,
                    skinHash = skin.hash,
                    size = bytes.size.toLong(),
                    createdAt = now,
                    lastAccess = now
                )
            )
        } catch (e: Throwable) {
            withContext(Dispatchers.IO) { file.delete() }
            throw e
        }
        prune()
        return bytes
    }

    suspend fun clearUuid(uuid: String) {
        val records = RenderCacheDao.deleteByUuid(uuid)
        withContext(Dispatchers.IO) {
            records.forEach { cacheFile(it.uuid, it.paramsMd5, it.ext).delete() }
            cacheDir(uuid).delete()
        }
    }

    private suspend fun prune() {
        val config = ConfigManager.config.renderCache
        val maxBytes = config.maxBytes
        val maxEntries = config.maxEntries
        if (maxBytes <= 0 || maxEntries <= 0) return

        val records = RenderCacheDao.listByLastAccess()
        var totalBytes = records.sumOf { it.size }
        var totalEntries = records.size
        if (totalBytes <= maxBytes && totalEntries <= maxEntries) return

        val targetBytes = (maxBytes * config.lowWatermarkRatio.coerceIn(0.1, 1.0)).toLong()
        val targetEntries = (maxEntries * config.lowWatermarkRatio.coerceIn(0.1, 1.0)).toInt().coerceAtLeast(1)
        for (record in records) {
            if (totalBytes <= targetBytes && totalEntries <= targetEntries) break
            cacheFile(record.uuid, record.paramsMd5, record.ext).delete()
            RenderCacheDao.delete(record.uuid, record.paramsMd5, record.ext)
            totalBytes -= record.size
            totalEntries--
        }
    }

    private suspend fun writeAtomically(file: File, bytes: ByteArray) = withContext(Dispatchers.IO) {
        file.parentFile.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.${ProcessHandle.current().pid()}.${System.nanoTime()}.tmp")
        tmp.writeBytes(bytes)
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun cacheFile(uuid: String, paramsMd5: String, ext: String): File =
        cacheDir(uuid).resolve("$paramsMd5.$ext")

    private fun cacheDir(uuid: String): File =
        File(ConfigManager.config.renderCache.dir).resolve(uuid)

    private data class CacheKey(val uuid: String, val paramsMd5: String, val ext: String)

    private class RenderLock(
        val mutex: Mutex = Mutex(),
        var references: Int = 0,
    )

    private fun String.md5(): String {
        val digest = MessageDigest.getInstance("MD5").digest(toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
