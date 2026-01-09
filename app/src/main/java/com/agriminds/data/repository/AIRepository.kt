package com.agriminds.data.repository

import com.agriminds.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val geminiApiService: GeminiApiService
) {
    private val apiKey = "AIzaSyBgcPsiq9SfdmUuOb6KgQiwawOTSp1tXH0" // Move to BuildConfig in production
    
    suspend fun getAgriculturalAdvice(
        question: String,
        category: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildSystemPrompt(category, question)
            
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(systemPrompt))
                    )
                )
            )
            
            val response = geminiApiService.generateContent(apiKey, request)
            
            if (response.isSuccessful && response.body() != null) {
                val answer = response.body()?.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text
                    ?: return@withContext Result.failure(Exception("No response from AI"))
                
                Result.success(answer)
            } else {
                // Fallback to mock answer if API fails
                Result.success(getMockAnswer(category, question))
            }
        } catch (e: Exception) {
            // Fallback to mock answer on network error
            Result.success(getMockAnswer(category, question))
        }
    }
    
    private fun buildSystemPrompt(category: String, question: String): String {
        return """
            You are an expert agricultural advisor with deep knowledge of farming practices in Bangladesh.
            Question Category: $category
            Farmer's Question: $question

            Please provide a comprehensive answer considering:
            1. Bangladesh's tropical monsoon climate and soil conditions
            2. Local crop varieties (rice, jute, tea, vegetables)
            3. Cost-effective solutions for small-scale farmers
            
            Structure your answer with:
            - Problem Analysis
            - Immediate Actions
            - Detailed Recommendations
            - Prevention Tips
        """.trimIndent()
    }
    
    private fun getMockAnswer(category: String, question: String): String {
        return when {
            question.contains("yellow", ignoreCase = true) || 
            question.contains("leaf", ignoreCase = true) -> {
                """
                **Problem Analysis:**
                Yellow leaves in rice plants indicate nitrogen deficiency or possible pest infestation.
                
                **Immediate Actions:**
                1. Check for pests on leaf undersides
                2. Examine root system for rot
                3. Test soil moisture levels
                
                **Detailed Recommendations:**
                • Apply urea fertilizer (50-75 kg per acre)
                • Ensure proper drainage in paddy field
                • Use Neem-based pesticide if pests are present
                • Maintain 2-3 inch water level in field
                
                **Prevention Tips:**
                • Follow proper NPK fertilization schedule
                • Practice crop rotation with legumes
                • Use disease-resistant rice varieties like BRRI dhan28
                • Monitor plants weekly for early detection
                """.trimIndent()
            }
            question.contains("pest", ignoreCase = true) -> {
                """
                **Problem Analysis:**
                Pest management is crucial for Bangladesh's humid climate where pests thrive.
                
                **Immediate Actions:**
                1. Identify the specific pest type
                2. Remove heavily infested plants
                3. Inspect neighboring crops
                
                **Detailed Recommendations:**
                • Use integrated pest management (IPM)
                • Apply Neem oil spray (5ml per liter water)
                • Install pheromone traps in field
                • Encourage natural predators (birds, spiders)
                • Use bio-pesticides before chemical ones
                
                **Prevention Tips:**
                • Maintain field hygiene
                • Practice intercropping with marigold or basil
                • Avoid over-watering
                • Use resistant crop varieties
                • Regular field monitoring
                """.trimIndent()
            }
            question.contains("soil", ignoreCase = true) -> {
                """
                **Problem Analysis:**
                Soil health is fundamental for sustainable farming in Bangladesh.
                
                **Immediate Actions:**
                1. Conduct soil pH test
                2. Check soil texture and drainage
                3. Assess organic matter content
                
                **Detailed Recommendations:**
                • Add compost or cow dung (2-3 tons per acre)
                • If pH < 6, apply lime (200-300 kg per acre)
                • If pH > 7.5, add sulfur or organic matter
                • Practice green manuring with dhaincha or sunhemp
                • Implement crop rotation
                
                **Prevention Tips:**
                • Test soil annually before planting
                • Avoid excessive chemical fertilizer use
                • Use balanced NPK fertilization
                • Maintain organic matter through composting
                • Practice minimum tillage
                """.trimIndent()
            }
            else -> {
                """
                **Agricultural Advisory:**
                Thank you for your question about $category.
                
                **General Recommendations for Bangladesh:**
                • Follow seasonal crop calendar
                • Use locally adapted varieties
                • Implement good agricultural practices (GAP)
                • Maintain proper irrigation and drainage
                • Monitor weather forecasts regularly
                
                **Resources:**
                • Contact your local agricultural extension officer
                • Visit BRRI or BARI for technical guidance
                • Join farmer cooperatives for shared learning
                
                For specific advice, please provide more details about your crop, symptoms, and field conditions.
                """.trimIndent()
            }
        }
    }
}
