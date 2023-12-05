package net.zhuruoling.nmsl.cache

import cn.hutool.core.io.FileUtil
import net.zhuruoling.nmsl.util.gson
import net.zhuruoling.nmsl.util.sha1
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Path
import kotlin.io.path.*

object CacheProvider {
    val cacheRoot = Path("./cache")
    val cacheFileMetaRoot = cacheRoot / "meta"
    val cacheDownloadRoot = cacheRoot / "download"
    private val logger = LoggerFactory.getLogger("Cache")
    private val caches = mutableMapOf<String, FileMetadata>()
    private val httpClient = HttpClient.newHttpClient()

    fun init() {
        logger.info("Building file cache.")
        listOf(cacheRoot, cacheDownloadRoot, cacheFileMetaRoot).forEach {
            if (it.notExists()) {
                it.createDirectories()
            }
        }
        FileUtil.ls(cacheDownloadRoot.toAbsolutePath().toString()).forEach {
            val fileNameWithExt = it.name
            if ((cacheFileMetaRoot / "$fileNameWithExt.json").notExists()) {
                logger.warn("Cannot find associated file metadata of $fileNameWithExt at $cacheFileMetaRoot.")
                it.delete()
            }
        }
        FileUtil.ls(cacheFileMetaRoot.toAbsolutePath().toString()).forEach {
            val fileNameWithExt = it.name
            if ((cacheDownloadRoot / fileNameWithExt.removeSuffix(".json")).notExists()) {
                logger.warn("Cannot find associated file of $fileNameWithExt at $cacheDownloadRoot.")
                it.delete()
            }
            try {
                it.reader().use { reader ->
                    gson.fromJson(reader, FileMetadata::class.java).apply {
                        caches[fileName] = this
                    }
                }
            } catch (e: Exception) {
                logger.warn("Cannot load file metadata from $it, caused by $e")
                it.delete()
            }
        }

    }

    fun downloadFile(meta: FileMetadata): Path {
        val expectFilePath = cacheDownloadRoot / meta.fileName
        if (meta.fileName in caches && expectFilePath.exists()) {
            val cachedMetadata = caches[meta.fileName]!!
            if (meta != cachedMetadata) {
                logger.warn("Provided metadata does not match with cached metadata, cached file will re-download.")
                downloadFile0(meta)
                return expectFilePath
            }
            if (!validateFile(expectFilePath, meta.fileHashSha1)) {
                logger.warn("Cached file sha1 does not match with metadata, cached file will re-download.")
                downloadFile0(meta)
                return expectFilePath
            }
            return expectFilePath
        } else {
            downloadFile0(meta)
            return expectFilePath
        }
    }

    private fun downloadFile0(meta: FileMetadata) {
        val outputFilePath = cacheDownloadRoot / meta.fileName
        val metadataFilePath = cacheFileMetaRoot / "${meta.fileName}.json"
        metadataFilePath.apply {
            deleteIfExists()
            createFile()
            writer().use {
                gson.toJson(meta, it)
            }
        }
        outputFilePath.apply {
            deleteIfExists()
            httpClient.send(
                HttpRequest.newBuilder(URL(meta.downloadUrl).toURI()).GET().build(),
                BodyHandlers.ofFile(this)
            )
        }
        if (!validateFile(outputFilePath, meta.fileHashSha1)) {
            throw RuntimeException(
                "Downloaded file $outputFilePath sha1 not match. (expect: ${
                    meta.fileHashSha1
                }, actual: ${
                    outputFilePath.toFile().sha1()
                }"
            )
        }
    }

    private fun validateFile(filePath: Path, expectSha1: String): Boolean {
        return filePath.toFile().sha1() == expectSha1
    }
}

data class FileMetadata(val fileName: String, val downloadUrl: String, val fileSize: Long, val fileHashSha1: String)