package moe.group13.routenode.ui.search

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import moe.group13.routenode.R
import moe.group13.routenode.utils.GptConfig

class AIAdviceManager(
    private val fragment: Fragment,
    private val viewModel: SearchViewModel,
    private val routeNodeAdapter: RouteNodeAdapter
) {

    fun askAIForAdvice() {
        // Get the current route node data from the adapter
        val routeNodeData = routeNodeAdapter.getRouteNodeData()

        // Read AI settings saved from AIModelsActivity
        val aiPrefs = fragment.requireContext().getSharedPreferences("ai_model_settings", Context.MODE_PRIVATE)
        val defaultConfig = GptConfig.DEFAULT_CONFIG

        val model = aiPrefs.getString("model", defaultConfig.model)
        val temperature = aiPrefs.getFloat(
            "temperature",
            defaultConfig.temperature.toFloat()
        ).toDouble()
        val maxTokens = aiPrefs.getInt(
            "max_tokens",
            defaultConfig.max_tokens
        )
        val responseLanguage = aiPrefs.getString("language", "English") ?: "English"

        // Read distance unit preference from settings
        val settingsPrefs = fragment.requireContext().getSharedPreferences("route_settings", Context.MODE_PRIVATE)
        val unitIndex = settingsPrefs.getInt("unit_index", 0)
        val distanceUnit = if (unitIndex == 1) "mi" else "km"

        // Call ViewModel with the actual user input data and AI settings
        viewModel.askAIForAdviceWithRouteNodes(
            routeNodeData = routeNodeData,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            distanceUnit = distanceUnit,
            responseLanguage = responseLanguage
        )
    }

    fun observeViewModel(
        lifecycleOwner: LifecycleOwner,
        onAiResponseReceived: () -> Unit,
        onLoadingFinished: () -> Unit
    ) {
        // Observe loading state
        viewModel.isLoading.observe(lifecycleOwner) { isLoading ->
            val button = fragment.view?.findViewById<MaterialButton>(R.id.buttonAskAI)
            // Only disable during loading
            button?.isEnabled = !isLoading
            button?.text = if (isLoading) "Loading..." else "Ask AI for advice !"

            // Update loading spinner in adapter
            routeNodeAdapter.setLoadingState(isLoading)

            // If loading finished, notify callback
            if (!isLoading) {
                onLoadingFinished()
            }
        }

        // Observe AI response
        viewModel.aiResponse.observe(lifecycleOwner) { response ->
            routeNodeAdapter.setAiResponse(response)
            
            // Scroll to bottom when AI response is received
            fragment.view?.findViewById<RecyclerView>(R.id.recyclerRouteNodes)?.post {
                val recycler = fragment.view?.findViewById<RecyclerView>(R.id.recyclerRouteNodes)
                recycler?.smoothScrollToPosition(routeNodeAdapter.itemCount - 1)
            }

            // Notify callback when response is received
            if (response != null && response.isNotBlank()) {
                onAiResponseReceived()
            }
        }

        // Observe errors
        viewModel.errorMessage.observe(lifecycleOwner) { error ->
            error?.let {
                Toast.makeText(fragment.requireContext(), it, Toast.LENGTH_LONG).show()
                onLoadingFinished()
            }
        }
    }
}