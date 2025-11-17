package moe.group13.routenode.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.group13.routenode.data.model.GptConfig
import moe.group13.routenode.data.model.GptRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun getCompletion(gptRequest: GptRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", gptRequest.prompt)
                })
            }
            
            val jsonBody = JSONObject().apply {
                put("model", gptRequest.model)
                put("messages", messagesArray)
                put("temperature", gptRequest.temperature)
                put("max_tokens", gptRequest.max_tokens)
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(GptConfig.ENDPOINT)
                .addHeader("Authorization", "Bearer ${GptConfig.OPENAI_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val content = message.getString("content")
                        Result.success(content.trim())
                    } else {
                        Result.failure(Exception("No response from AI"))
                    }
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(Exception("API Error: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}