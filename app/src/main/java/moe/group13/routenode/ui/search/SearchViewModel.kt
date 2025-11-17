package moe.group13.routenode.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.group13.routenode.data.model.GptConfig
import moe.group13.routenode.data.repository.OpenAiService

class SearchViewModel(
    private val openAiService: OpenAiService = OpenAiService()
) : ViewModel() {
    
    val aiResponse = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>()
    
    fun askAIForAdvice() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null
                
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
}