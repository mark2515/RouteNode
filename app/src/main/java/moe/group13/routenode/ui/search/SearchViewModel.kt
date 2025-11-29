package moe.group13.routenode.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.group13.routenode.utils.GptConfig
import moe.group13.routenode.data.repository.OpenAiService
import moe.group13.routenode.data.repository.PromptBuilder

class SearchViewModel(
    private val openAiService: OpenAiService = OpenAiService(),
    private val promptBuilder: PromptBuilder = PromptBuilder(openAiService)
) : ViewModel() {
    
    val aiResponse = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>()
    
    fun askAIForAdvice() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null
                aiResponse.value = ""
                
                val gptRequest = GptConfig.GPT_INIT.firstOrNull()
                
                if (gptRequest != null) {
                    val result = openAiService.getCompletion(gptRequest)
                    
                    result.onSuccess { response ->
                        aiResponse.value = response
                    }.onFailure { exception ->
                        errorMessage.value = "Failed to get AI response: ${exception.message}"
                    }
                } else {
                    errorMessage.value = "No GPT configuration found"
                }
            } catch (e: Exception) {
                errorMessage.value = "Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun askAIWithCustomPrompt(prompt: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null
                aiResponse.value = ""
                
                // Get default config
                val defaultConfig = GptConfig.GPT_INIT.firstOrNull()
                
                if (defaultConfig != null) {
                    val customRequest = defaultConfig.copy(prompt = prompt)
                    val result = openAiService.getCompletion(customRequest)
                    
                    result.onSuccess { response ->
                        aiResponse.value = response
                    }.onFailure { exception ->
                        errorMessage.value = "Failed to get AI response: ${exception.message}"
                    }
                } else {
                    errorMessage.value = "No GPT configuration found"
                }
            } catch (e: Exception) {
                errorMessage.value = "Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun askAIForAdviceWithRouteNodes(
        routeNodeData: List<RouteNodeAdapter.RouteNodeData>,
        model: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null,
        distanceUnit: String = "km",
        responseLanguage: String = "English"
    ) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null
                aiResponse.value = ""
                
                val result = promptBuilder.buildAndSendPromptFromAdapterData(
                    routeNodeData = routeNodeData,
                    model = model,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    distanceUnit = distanceUnit,
                    responseLanguage = responseLanguage
                )
                
                result.onSuccess { response ->
                    aiResponse.value = response
                }.onFailure { exception ->
                    errorMessage.value = "Failed to get AI response: ${exception.message}"
                }
            } catch (e: Exception) {
                errorMessage.value = "Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun clearAiResponse() {
        aiResponse.value = null
    }
}