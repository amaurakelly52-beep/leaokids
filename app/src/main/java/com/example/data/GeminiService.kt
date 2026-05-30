package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Moshi Models for Gemini API ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "schema") val schema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "text") val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseFormat") val responseFormat: ResponseFormat? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    @Json(name = "parts") val parts: List<PartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: ContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

// --- Domain Model for Content Moderation output ---

@JsonClass(generateAdapter = true)
data class ModerationResult(
    @Json(name = "safe") val safe: Boolean,
    @Json(name = "reason") val reason: String,
    @Json(name = "suggestedAlternative") val suggestedAlternative: String
)

// --- Retrofit API Interface ---

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Direct Net Service Wrapper ---

object GeminiService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GeminiApi::class.java)

    /**
     * Audit search terms or videos using Gemini model for smart parental filter.
     */
    suspend fun auditContent(input: String): ModerationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback filter if API Key is not configured yet
            return@withContext evaluateLocally(input)
        }

        val prompt = """
            Verifique se o seguinte título de vídeo, palavra de busca ou conteúdo de canal é recomendável para crianças pequenas (Livre de violência, monstros assustadores, terror, teor adulto, mortes, armas ou pegadinhas perigosas).
            Termo a analisar: "$input"
            
            Responda exclusivamente no formato JSON com os campos descritos abaixo:
            {
              "safe": true ou false,
              "reason": "Explicação amigável em português explicando por que é seguro ou por que foi bloqueado.",
              "suggestedAlternative": "Uma alternativa divertida e educativa recomendando astronomia, dinossauros ou ciências no Leão Kids."
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.1f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "Você é o Leãozinho, o mascote e assistente IA de curadoria infantil do aplicativo Leão Kids. Sua missão é garantir 100% de segurança de uso para as crianças.")))
        )

        try {
            val response = api.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(ModerationResult::class.java)
                adapter.fromJson(jsonText) ?: evaluateLocally(input)
            } else {
                evaluateLocally(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            evaluateLocally(input)
        }
    }

    /**
     * Robust local moderation fallback check. Matches key Portuguese phrases.
     */
    fun evaluateLocally(input: String): ModerationResult {
        val lowercaseInput = input.lowercase().trim()
        val bannedKeywords = listOf(
            "terror", "horror", "sangue", "morte", "matar", "violência", "arma", "crime", "luta", "combate",
            "assustador", "demônio", "fantasma", "suicídio", "veneno", "monstro", "monster", "school", "truck",
            "inc", "slender", "momo", "chucky", "gore", "assalto", "roubo", "tiro", "faca", "guerra", "adulto", "sex"
        )

        for (banned in bannedKeywords) {
            if (lowercaseInput.contains(banned)) {
                return ModerationResult(
                    safe = false,
                    reason = "O termo '$input' contém a palavra protegida '$banned' identificada no filtro de segurança automático.",
                    suggestedAlternative = "Que tal pesquisar sobre 'Sistema Solar', 'T-Rex' ou 'Manual do Mundo'?"
                )
            }
        }

        return ModerationResult(
            safe = true,
            reason = "Aprovado pelo filtro rápido de termos locais.",
            suggestedAlternative = ""
        )
    }
}
