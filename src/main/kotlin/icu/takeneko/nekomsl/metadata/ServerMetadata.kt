package icu.takeneko.nekomsl.metadata

import kotlinx.serialization.Serializable

@Serializable
data class ServerMetadata(
    val metadataVersion: Int,
    val baseUrl: String,
    val servers: Map<String, String>
)
