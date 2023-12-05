package net.zhuruoling.nmsl.cache

import kotlin.io.path.Path
import kotlin.io.path.div

object CacheProvider {
    val cacheRoot = Path("./cache")
    val cacheFileMetaRoot = cacheRoot / "meta"
    val cacheDownloadRoot = cacheRoot / "download"

    fun downloadFile(url:String, outputFileName:String){

    }
}

data class FileMetadata(val fileName: String, val downloadUrl: String, val fileSize: Long, val fileHash: String)