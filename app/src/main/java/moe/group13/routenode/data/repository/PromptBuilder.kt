package moe.group13.routenode.data.repository

import moe.group13.routenode.utils.GptConfig
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
        
        promptBuilder.append("I need help planning a route with the following stops:\n")

        routeNodes.forEachIndexed { index, node ->
            promptBuilder.append("Stop: ${node.no}\n")
            promptBuilder.append("Location: ${node.location}\n")
            promptBuilder.append("The place you're looking for: ${node.placeYouAreLookingFor}\n")
            promptBuilder.append("Distance: ${node.distance}\n")
            
            if (node.additionalRequirements.trim().isNotEmpty()) {
                promptBuilder.append("Additional requirements: ${node.additionalRequirements}\n")
            }
            
            if (index < routeNodes.size - 1) {
                promptBuilder.append("\n")
            }
        }

        promptBuilder.append("\n")
        promptBuilder.append("The number after 'Stop' indicates the order of the user's destinations. For example, 'Stop: 3' means it is the user's third destination.\n")
        promptBuilder.append("'Location' represents the specific address the user wants to go to.\n")
        promptBuilder.append("'The place you're looking for' refers to the type of place the user wants to find near the Location.\n")
        promptBuilder.append("'Distance' means how far the place should be from the Location.\n")
        promptBuilder.append("For example, if the user enters 'Location: Simon Fraser University, University Drive West, Burnaby, BC, Canada', 'The place you’re looking for: an Instagrammable café', and 'Distance: 5', it means the user wants to find an Instagrammable café within 5 km of SFU.\n")
        promptBuilder.append("'Additional requirements' refers to anything extra the user wants to add.\n")
        promptBuilder.append("\n")
        promptBuilder.append("Based on these requirements, please provide:\n")
        promptBuilder.append("Recommended places that match the criteria for each stop.\n")
        promptBuilder.append("Any helpful tips or suggestions for visiting these places.\n")
        promptBuilder.append("Please put the addresses of the places you recommended in a code block so I can copy them.\n")
        promptBuilder.append("You should provide the recommended places in the order of the stop numbers.\n")
        
        promptBuilder.append("\n")
        promptBuilder.append("Input Example:\n")
        promptBuilder.append("Stop: 1\n")
        promptBuilder.append("Location: Simon Fraser University, University Drive West, Burnaby, BC, Canada\n")
        promptBuilder.append("The place you're looking for: a pizza restaurant\n")
        promptBuilder.append("Distance: 2\n")
        promptBuilder.append("\n")
        promptBuilder.append("Stop: 3\n")
        promptBuilder.append("Location: Metrotown, Burnaby, BC, Canadaa\n")
        promptBuilder.append("The place you're looking for: a sushi restaurant\n")
        promptBuilder.append("Distance: 8\n")
        promptBuilder.append("\n")
        promptBuilder.append("Stop: 2\n")
        promptBuilder.append("Location: Lafarge Lake, Coquitlam, BC, Canada\n")
        promptBuilder.append("The place you're looking for: an Instagrammable café\n")
        promptBuilder.append("Distance: 5\n")
        promptBuilder.append("Additional requirements: quiet, good natural light\n")

        promptBuilder.append("\n")
        promptBuilder.append("Output Example:\n")
        promptBuilder.append("**Stop 1— Near Simon Fraser University (pizza restaurant within 2 km)**\n")
        promptBuilder.append("Recommended Place:\n")
        promptBuilder.append("```\n")
        promptBuilder.append("Uncle Fatih's Pizza - SFU, 9055 University High St Unit 108, Burnaby, BC V5A 0A7\n")
        promptBuilder.append("```\n\n")
        promptBuilder.append("Tips:\n")
        promptBuilder.append("- Try their spinach & feta or garlic chicken slices.\n")
        promptBuilder.append("- Usually not too busy in the afternoon.\n")
        promptBuilder.append("\n")
        promptBuilder.append("**Stop 2 — Near Lafarge Lake (Instagrammable café within 5 km, quiet, good natural light)**\n")
        promptBuilder.append("Recommended Place:\n")
        promptBuilder.append("```\n")
        promptBuilder.append("Caffé Divano, 3003 Burlington Dr, Coquitlam, BC V3B 7T8\n")
        promptBuilder.append("```\n\n")
        promptBuilder.append("Tips:\n")
        promptBuilder.append("- Best lighting from 11 AM to 2 PM.\n")
        promptBuilder.append("- Their matcha latte and pastries are very photogenic.\n")
        promptBuilder.append("\n")
        promptBuilder.append("**Stop 3 — Near Metrotown (sushi restaurant within 8 km)**\n")
        promptBuilder.append("Recommended Place:\n")
        promptBuilder.append("```\n")
        promptBuilder.append("Sushi Garden Metro, Kingsway, Burnaby, BC\n")
        promptBuilder.append("```\n\n")
        promptBuilder.append("Tips:\n")
        promptBuilder.append("- Expect a short wait during peak dinner hours.\n")
        promptBuilder.append("- The salmon sashimi and House Roll are customer favourites.\n")
        
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
        val gptRequest = moe.group13.routenode.utils.GptRequest(
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