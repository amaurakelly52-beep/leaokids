package com.example.data


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
import java.net.URLEncoder
import java.util.regex.Pattern

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
    fun getGeminiApiKey(): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: "MY_GEMINI_API_KEY"
        } catch (e: Exception) {
            "MY_GEMINI_API_KEY"
        }
    }

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
        val apiKey = getGeminiApiKey()
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

    /**
     * Search YouTube videos via Gemini AI.
     */
    suspend fun searchYoutubeViaAI(query: String): List<KidVideo> = withContext(Dispatchers.IO) {
        val apiKey = getGeminiApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext emptyList()
        }

        val prompt = """
            Você é um assistente de busca do YouTube Kids. O usuário quer buscar vídeos sobre: "$query". 
            Encontre 5 vídeos reais e seguros do YouTube que correspondam a essa busca (ex: vídeos reais de canais infantis populares).
            Retorne exclusivamente um objeto JSON com os campos descritos abaixo (sem Markdown, sem blocos de código):
            {
              "videos": [
                {
                  "id": "ID do vídeo do YouTube de 11 caracteres",
                  "title": "Título legível do vídeo",
                  "channelName": "Nome do canal oficial do vídeo",
                  "description": "Uma breve descrição do que acontece no vídeo",
                  "durationText": "Duração do vídeo (ex: 5:40)",
                  "category": "Categoria sugerida para o vídeo (ex: Ciências, Astronomia, Dinossauros, Música, etc.)"
                }
              ]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.5f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "Você é um gerador de resultados de busca do YouTube Kids, retornando apenas JSON estruturado de vídeos reais e seguros.")))
        )

        try {
            val response = api.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(YoutubeSearchResponse::class.java)
                val searchResponse = adapter.fromJson(jsonText)
                searchResponse?.videos?.map { item ->
                    KidVideo(
                        id = item.id,
                        title = item.title,
                        channelName = item.channelName,
                        thumbnailUrl = "https://img.youtube.com/vi/${item.id}/hqdefault.jpg",
                        durationText = item.durationText,
                        category = item.category,
                        description = item.description
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Search YouTube videos publicly using a web scraper that extracts video ID, title, and channel.
     */
    suspend fun searchYoutubePublicly(query: String): List<KidVideo> = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
            .build()
        try {
            val response = okHttpClient.newCall(request).execute()
            val html = response.body?.string() ?: ""
            
            val videoList = mutableListOf<KidVideo>()
            val videoRendererMarker = "\"videoRenderer\":{"
            var index = 0
            var count = 0
            
            while (count < 8) {
                index = html.indexOf(videoRendererMarker, index)
                if (index == -1) break
                
                val endLimit = minOf(html.length, index + 3000)
                val block = html.substring(index, endLimit)
                
                val idMatcher = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").matcher(block)
                if (idMatcher.find()) {
                    val id = idMatcher.group(1)
                    
                    val titleMatcher = Pattern.compile("\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"").matcher(block)
                    if (titleMatcher.find()) {
                        var title = titleMatcher.group(1) ?: ""
                        title = title.replace("\\u0026", "&")
                                     .replace("\\\"", "\"")
                                     .replace("\\\\", "\\")
                        
                        var channel = "YouTube"
                        val bylineMatcher = Pattern.compile("\"longBylineText\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"").matcher(block)
                        if (bylineMatcher.find()) {
                            channel = bylineMatcher.group(1) ?: "YouTube"
                        } else {
                            val ownerMatcher = Pattern.compile("\"ownerText\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"").matcher(block)
                            if (ownerMatcher.find()) {
                                channel = ownerMatcher.group(1) ?: "YouTube"
                            }
                        }
                        channel = channel.replace("\\u0026", "&")
                                         .replace("\\\"", "\"")
                                         .replace("\\\\", "\\")
                        
                        if (id != null && title.isNotEmpty()) {
                            videoList.add(
                                KidVideo(
                                    id = id,
                                    title = title,
                                    channelName = channel,
                                    thumbnailUrl = "https://img.youtube.com/vi/$id/hqdefault.jpg",
                                    durationText = "Vídeo",
                                    category = "YouTube",
                                    description = "Vídeo real do YouTube."
                                )
                            )
                            count++
                        }
                    }
                }
                index += videoRendererMarker.length
            }
            videoList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Gets title, channel and description for a specific video ID using Gemini AI curation.
     */
    suspend fun getVideoDetailsViaAI(videoId: String, pageTitle: String): KidVideo = withContext(Dispatchers.IO) {
        val apiKey = getGeminiApiKey()
        val cleanTitle = pageTitle.replace(" - YouTube", "").replace(" - YouTube Mobile", "").trim()
        
        val defaultVideo = KidVideo(
            id = videoId,
            title = cleanTitle,
            channelName = "YouTube",
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            durationText = "Vídeo",
            category = "Geral",
            description = "Vídeo liberado do YouTube."
        )

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext defaultVideo
        }

        val prompt = """
            Dado o ID do vídeo do YouTube "$videoId" e o título bruto "$cleanTitle", retorne detalhes limpos sobre o vídeo para uso infantil (ex: canal real, título limpo, categoria adequada).
            Retorne exclusivamente um objeto JSON (sem Markdown, sem blocos de código):
            {
              "title": "Título limpo e amigável do vídeo",
              "channelName": "Nome do canal oficial do vídeo",
              "description": "Uma breve descrição amigável para crianças",
              "category": "Uma destas categorias: Ciências, Astronomia, Dinossauros, Música, Artes, Desenhos, Geral"
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "Você é um formatador de metadados de vídeos do YouTube Kids, retornando apenas JSON estruturado.")))
        )

        try {
            val response = api.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(Map::class.java)
                val map = adapter.fromJson(jsonText)
                if (map != null) {
                    return@withContext KidVideo(
                        id = videoId,
                        title = map["title"]?.toString() ?: cleanTitle,
                        channelName = map["channelName"]?.toString() ?: "YouTube",
                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        durationText = "Vídeo",
                        category = map["category"]?.toString() ?: "Geral",
                        description = map["description"]?.toString() ?: "Vídeo liberado do YouTube."
                    )
                }
            }
            defaultVideo
        } catch (e: Exception) {
            e.printStackTrace()
            defaultVideo
        }
    }

    /**
     * Extracts video ID from standard YouTube mobile or desktop URLs.
     */
    fun extractYoutubeVideoId(url: String?): String? {
        if (url == null) return null
        val pattern = "(?:v=|\\/embed\\/|\\/v\\/|youtu\\.be\\/|\\/watch\\?v=|\\&v=)([^#\\&\\?]{11})"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}

@JsonClass(generateAdapter = true)
data class YoutubeVideoItem(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "channelName") val channelName: String,
    @Json(name = "description") val description: String,
    @Json(name = "durationText") val durationText: String,
    @Json(name = "category") val category: String
)

@JsonClass(generateAdapter = true)
data class YoutubeSearchResponse(
    @Json(name = "videos") val videos: List<YoutubeVideoItem>
)
