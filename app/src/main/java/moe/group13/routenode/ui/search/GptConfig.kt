package moe.group13.routenode.data.model
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

object GptConfig {
    val OPENAI_API_KEY: String = BuildConfig.OPENAI_API_KEY
    val ENDPOINT: String = BuildConfig.OPENAI_ENDPOINT
    
    val GPT_INIT = listOf(
        GptRequest(
            model = "gpt-4-turbo",
            prompt = "Tell me a pizza restaurant near SFU.",
            temperature = 0.7,
            max_tokens = 400
        )
    )
}