package ir.hrka.hooshmand.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for the decoded `model.json` payload.
 *
 * @property models Published on-device model entries.
 */
@Serializable
internal data class NetworkModelManifest(
    val models: List<NetworkModelEntry>,
)

/**
 * Network DTO for one model entry in `model.json`.
 *
 * @property modelName Output `.litertlm` file name.
 * @property singlePartAddress Single-file download URL, or `null`.
 * @property multiPartAddress Ordered multipart download URLs.
 */
@Serializable
internal data class NetworkModelEntry(
    @SerialName("model_name") val modelName: String,
    @SerialName("single_part_address") val singlePartAddress: String? = null,
    @SerialName("multi_part_address") val multiPartAddress: List<String> = emptyList(),
)
