package moe.group13.routenode.utils
import moe.group13.routenode.BuildConfig

data class GptRequest(
    val model: String,
    val prompt: String,
    val temperature: Double,
    val max_tokens: Int
)

data class GptResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
) {
    data class Choice(
        val text: String = "",
        val index: Int = 0,
        val finish_reason: String = ""
    )
    
    data class Usage(
        val prompt_tokens: Int = 0,
        val completion_tokens: Int = 0,
        val total_tokens: Int = 0
    )
}

data class GptConfigParams(
    val model: String,
    val temperature: Double,
    val max_tokens: Int
)

object GptConfig {
    val OPENAI_API_KEY: String = BuildConfig.OPENAI_API_KEY
    val ENDPOINT: String = BuildConfig.OPENAI_ENDPOINT
    
    val DEFAULT_CONFIG = GptConfigParams(
        model = "gpt-4-turbo",
        temperature = 0.7,
        max_tokens = 800
    )
    
    @Deprecated("Use DEFAULT_CONFIG")
    val GPT_INIT = listOf(
        GptRequest(
            model = DEFAULT_CONFIG.model,
            prompt = "",  // prompts come from PromptBuilder
            temperature = DEFAULT_CONFIG.temperature,
            max_tokens = DEFAULT_CONFIG.max_tokens
        )
    )
}