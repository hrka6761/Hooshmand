package ir.hrka.llm.runtime.api

/**
 * Hardware accelerator used to run the LLM.
 *
 * Maps to LiteRT-LM [com.google.ai.edge.litertlm.Backend] backends.
 */
enum class LlmAccelerator {
    /** CPU inference. Most compatible, slowest. */
    CPU,

    /** GPU inference. Default for most models. */
    GPU,

    /** Neural Processing Unit. Requires device support and native libraries. */
    NPU,

    /** Tensor Processing Unit. Uses NPU backend internally. */
    TPU,
}
