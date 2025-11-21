package moe.group13.routenode.data.repository

import moe.group13.routenode.data.model.GptConfig
import moe.group13.routenode.ui.search.RouteNodeAdapter

class PromptBuilder(private val openAiService: OpenAiService = OpenAiService()) {

    data class RouteNodeInput(
        val no: Int,
        val location: String,
        val placeYouAreLookingFor: String,
        val distance: String,
        val additionalRequirements: String
    )
    
    fun buildPrompt(routeNodes: List<RouteNodeInput>): String {
        val promptBuilder = StringBuilder()
        
        promptBuilder.append("I need help planning a route with the following stops:\n\n")
        
        routeNodes.forEachIndexed { index, node ->
            promptBuilder.append("Stop ${node.no}:\n")
            promptBuilder.append("Location: ${node.location}\n")
            promptBuilder.append("The place you're looking for: ${node.placeYouAreLookingFor}\n")
            promptBuilder.append("Distance: ${node.distance}\n")
            
            if (node.additionalRequirements.trim().isNotEmpty()) {
                promptBuilder.append("- Additional requirements: ${node.additionalRequirements}\n")
            }
            
            if (index < routeNodes.size - 1) {
                promptBuilder.append("\n")
            }
        }
        
        promptBuilder.append("\n")
        promptBuilder.append("Based on these requirements, please provide:\n")
        promptBuilder.append("Recommended places that match the criteria for each stop\n")
        
        return promptBuilder.toString()
    }
    
    fun convertFromAdapterData(routeNodeData: List<RouteNodeAdapter.RouteNodeData>): List<RouteNodeInput> {
        return routeNodeData.map { data ->
            RouteNodeInput(
                no = data.no,
                location = data.location,
                placeYouAreLookingFor = data.place,
                distance = data.distance,
                additionalRequirements = data.additionalRequirements
            )
        }
    }
    
    suspend fun buildAndSendPrompt(
        routeNodes: List<RouteNodeInput>,
        model: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): Result<String> {
        // Validate inputs
        if (routeNodes.isEmpty()) {
            return Result.failure(Exception("No route nodes provided"))
        }
        
        // Validate each route node has required fields
        routeNodes.forEach { node ->
            if (node.location.trim().isEmpty()) {
                return Result.failure(Exception("Location is required for Route Node ${node.no}"))
            }
            if (node.placeYouAreLookingFor.trim().isEmpty()) {
                return Result.failure(Exception("The place you're looking for is required for Route Node ${node.no}"))
            }
            if (node.distance.trim().isEmpty()) {
                return Result.failure(Exception("Distance is required for Route Node ${node.no}"))
            }
        }
        
        // Get configuration
        val defaultConfig = GptConfig.DEFAULT_CONFIG
        
        // Build the prompt
        val prompt = buildPrompt(routeNodes)
        
        // Create the GPT request
        val gptRequest = moe.group13.routenode.data.model.GptRequest(
            model = model ?: defaultConfig.model,
            prompt = prompt,
            temperature = temperature ?: defaultConfig.temperature,
            max_tokens = maxTokens ?: defaultConfig.max_tokens
        )
        
        // Send to OpenAI service
        return openAiService.getCompletion(gptRequest)
    }
    
    suspend fun buildAndSendPromptFromAdapterData(
        routeNodeData: List<RouteNodeAdapter.RouteNodeData>,
        model: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): Result<String> {
        val routeNodes = convertFromAdapterData(routeNodeData)
        return buildAndSendPrompt(routeNodes, model, temperature, maxTokens)
    }
}