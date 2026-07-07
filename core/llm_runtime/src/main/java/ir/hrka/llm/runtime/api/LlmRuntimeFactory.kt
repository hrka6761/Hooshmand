package ir.hrka.llm.runtime.api

import android.content.Context
import ir.hrka.llm.runtime.internal.LiteRtLlmRuntime

/**
 * Factory for creating [LlmRuntime] instances.
 *
 * Each instance manages its own engine and conversation. Create one instance per active model.
 */
object LlmRuntimeFactory {
    /**
     * Creates a new [LlmRuntime] backed by LiteRT-LM.
     *
     * @param context Application or activity context. The application context is used internally.
     */
    fun create(context: Context): LlmRuntime {
        return LiteRtLlmRuntime(context.applicationContext)
    }
}
