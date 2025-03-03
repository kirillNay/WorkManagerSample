package nay.kirill.workmanagersample

import kotlinx.serialization.Serializable

@Serializable
data class FormData(
    val name: String,
    val surname: String,
    val description: String
)