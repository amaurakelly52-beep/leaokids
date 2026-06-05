package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Supabase REST API Models ---

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "name") val name: String,
    @Json(name = "is_boy") val isBoy: Boolean,
    @Json(name = "avatar_url") val avatarUrl: String,
    @Json(name = "creation_time") val creationTime: Long,
    @Json(name = "parent_email") val parentEmail: String,
    @Json(name = "screen_time_limit_minutes") val screenTimeLimitMinutes: Int = 60,
    @Json(name = "is_strict_channel_mode") val isStrictChannelMode: Boolean = false,
    @Json(name = "is_smart_curator_mode") val isSmartCuratorMode: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SupabaseConfig(
    @Json(name = "connected_email") val connectedEmail: String,
    @Json(name = "pin_code") val pinCode: String,
    @Json(name = "connected_name") val connectedName: String?,
    @Json(name = "connected_photo") val connectedPhoto: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseApprovedVideo(
    @Json(name = "id") val id: String,
    @Json(name = "profile_id") val profileId: Long = 0,
    @Json(name = "parent_email") val parentEmail: String,
    @Json(name = "title") val title: String,
    @Json(name = "channel_name") val channelName: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String,
    @Json(name = "duration_text") val durationText: String,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String
)

@JsonClass(generateAdapter = true)
data class SupabaseBlockedWord(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "word") val word: String,
    @Json(name = "profile_id") val profileId: Long = 0,
    @Json(name = "parent_email") val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabaseBlockedChannel(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "channel_name") val channelName: String,
    @Json(name = "profile_id") val profileId: Long = 0,
    @Json(name = "parent_email") val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAllowedChannel(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "channel_name") val channelName: String,
    @Json(name = "profile_id") val profileId: Long = 0,
    @Json(name = "parent_email") val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabasePlaylist(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "profile_id") val profileId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "parent_email") val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabasePlaylistVideo(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "playlist_id") val playlistId: Long,
    @Json(name = "video_id") val videoId: String,
    @Json(name = "title") val title: String,
    @Json(name = "channel_name") val channelName: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String,
    @Json(name = "parent_email") val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFavorite(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "profile_id") val profileId: Long,
    @Json(name = "video_id") val videoId: String,
    @Json(name = "title") val title: String,
    @Json(name = "channel_name") val channelName: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "parent_email") val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabaseHistory(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "profile_id") val profileId: Long,
    @Json(name = "video_id") val videoId: String,
    @Json(name = "title") val title: String,
    @Json(name = "channel_name") val channelName: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "watched_duration_seconds") val watchedDurationSeconds: Long,
    @Json(name = "parent_email") val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class SupabaseSearchAttempt(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "profile_id") val profileId: Long,
    @Json(name = "query") val query: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "parent_email") val parentEmail: String
)

// --- Retrofit Endpoints ---

interface SupabaseApi {
    // Profiles
    @GET("rest/v1/children_profiles")
    suspend fun getProfiles(@Query("parent_email") emailFilter: String): List<SupabaseProfile>

    @POST("rest/v1/children_profiles")
    suspend fun upsertProfile(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body profile: List<SupabaseProfile>
    ): Response<Unit>

    @DELETE("rest/v1/children_profiles")
    suspend fun deleteProfile(
        @Query("id") idFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Parent Config
    @GET("rest/v1/parent_config")
    suspend fun getConfig(@Query("connected_email") emailFilter: String): List<SupabaseConfig>

    @POST("rest/v1/parent_config")
    suspend fun upsertConfig(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body config: List<SupabaseConfig>
    ): Response<Unit>

    // Approved Videos
    @GET("rest/v1/approved_videos")
    suspend fun getApprovedVideos(@Query("parent_email") emailFilter: String): List<SupabaseApprovedVideo>

    @POST("rest/v1/approved_videos")
    suspend fun upsertApprovedVideo(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body video: List<SupabaseApprovedVideo>
    ): Response<Unit>

    @DELETE("rest/v1/approved_videos")
    suspend fun deleteApprovedVideo(
        @Query("id") idFilter: String,
        @Query("profile_id") profileIdFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Blocked Words
    @GET("rest/v1/blocked_words")
    suspend fun getBlockedWords(@Query("parent_email") emailFilter: String): List<SupabaseBlockedWord>

    @POST("rest/v1/blocked_words")
    suspend fun upsertBlockedWord(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body word: List<SupabaseBlockedWord>
    ): Response<Unit>

    @DELETE("rest/v1/blocked_words")
    suspend fun deleteBlockedWord(
        @Query("id") idFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Blocked Channels
    @GET("rest/v1/blocked_channels")
    suspend fun getBlockedChannels(@Query("parent_email") emailFilter: String): List<SupabaseBlockedChannel>

    @POST("rest/v1/blocked_channels")
    suspend fun upsertBlockedChannel(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body channel: List<SupabaseBlockedChannel>
    ): Response<Unit>

    @DELETE("rest/v1/blocked_channels")
    suspend fun deleteBlockedChannel(
        @Query("id") idFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Allowed Channels
    @GET("rest/v1/allowed_channels")
    suspend fun getAllowedChannels(@Query("parent_email") emailFilter: String): List<SupabaseAllowedChannel>

    @POST("rest/v1/allowed_channels")
    suspend fun upsertAllowedChannel(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body channel: List<SupabaseAllowedChannel>
    ): Response<Unit>

    @DELETE("rest/v1/allowed_channels")
    suspend fun deleteAllowedChannel(
        @Query("id") idFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Playlists
    @GET("rest/v1/playlists")
    suspend fun getPlaylists(@Query("parent_email") emailFilter: String): List<SupabasePlaylist>

    @POST("rest/v1/playlists")
    suspend fun upsertPlaylist(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body playlist: List<SupabasePlaylist>
    ): Response<Unit>

    @DELETE("rest/v1/playlists")
    suspend fun deletePlaylist(
        @Query("id") idFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Playlist Videos
    @GET("rest/v1/playlist_videos")
    suspend fun getPlaylistVideos(@Query("parent_email") emailFilter: String): List<SupabasePlaylistVideo>

    @POST("rest/v1/playlist_videos")
    suspend fun upsertPlaylistVideo(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body video: List<SupabasePlaylistVideo>
    ): Response<Unit>

    @DELETE("rest/v1/playlist_videos")
    suspend fun deletePlaylistVideo(
        @Query("playlist_id") playlistIdFilter: String,
        @Query("video_id") videoIdFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Favorites
    @GET("rest/v1/favorites")
    suspend fun getFavorites(@Query("parent_email") emailFilter: String): List<SupabaseFavorite>

    @POST("rest/v1/favorites")
    suspend fun upsertFavorite(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body favorite: List<SupabaseFavorite>
    ): Response<Unit>

    @DELETE("rest/v1/favorites")
    suspend fun deleteFavorite(
        @Query("profile_id") profileIdFilter: String,
        @Query("video_id") videoIdFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // History
    @GET("rest/v1/history")
    suspend fun getHistory(@Query("parent_email") emailFilter: String): List<SupabaseHistory>

    @POST("rest/v1/history")
    suspend fun upsertHistory(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body history: List<SupabaseHistory>
    ): Response<Unit>

    @DELETE("rest/v1/history")
    suspend fun deleteHistory(
        @Query("profile_id") profileIdFilter: String,
        @Query("parent_email") emailFilter: String
    ): Response<Unit>

    // Search History / Attempts
    @GET("rest/v1/search_history")
    suspend fun getSearchAttempts(@Query("parent_email") emailFilter: String): List<SupabaseSearchAttempt>

    @POST("rest/v1/search_history")
    suspend fun upsertSearchAttempt(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body attempt: List<SupabaseSearchAttempt>
    ): Response<Unit>

    @DELETE("rest/v1/search_history")
    suspend fun deleteSearchAttempts(
        @Query("parent_email") emailFilter: String
    ): Response<Unit>
}

// --- Service Wrapper ---

object SupabaseService {

    fun getSupabaseUrl(): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("SUPABASE_URL")
            val url = field.get(null) as? String ?: ""
            if (url == "SUA_URL_DO_SUPABASE_AQUI") "" else url
        } catch (e: Exception) {
            ""
        }
    }

    fun getSupabaseAnonKey(): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("SUPABASE_ANON_KEY")
            val key = field.get(null) as? String ?: ""
            if (key == "SUA_CHAVE_ANON_DO_SUPABASE_AQUI") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()
        return url.isNotEmpty() && key.isNotEmpty()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", getSupabaseAnonKey())
                    .addHeader("Authorization", "Bearer ${getSupabaseAnonKey()}")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            })
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val api: SupabaseApi? by lazy {
        if (isConfigured()) {
            var baseUrl = getSupabaseUrl()
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/"
            }
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SupabaseApi::class.java)
        } else {
            null
        }
    }

    // Profiles Pull / Push / Delete
    suspend fun fetchProfiles(email: String): List<SupabaseProfile> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getProfiles("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushProfile(profile: SupabaseProfile) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertProfile(profile = listOf(profile))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteProfile(id: Long, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteProfile("eq.$id", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Config Pull / Push
    suspend fun fetchConfig(email: String): SupabaseConfig? = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext null
        try {
            client.getConfig("eq.$email").firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun pushConfig(config: SupabaseConfig) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertConfig(config = listOf(config))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Approved Videos Pull / Push / Delete
    suspend fun fetchApprovedVideos(email: String): List<SupabaseApprovedVideo> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getApprovedVideos("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushApprovedVideo(video: SupabaseApprovedVideo) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertApprovedVideo(video = listOf(video))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteApprovedVideo(id: String, profileId: Long, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteApprovedVideo("eq.$id", "eq.$profileId", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Blocked Words Pull / Push / Delete
    suspend fun fetchBlockedWords(email: String): List<SupabaseBlockedWord> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getBlockedWords("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushBlockedWord(word: SupabaseBlockedWord) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertBlockedWord(word = listOf(word))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteBlockedWord(id: Long, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteBlockedWord("eq.$id", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Blocked Channels Pull / Push / Delete
    suspend fun fetchBlockedChannels(email: String): List<SupabaseBlockedChannel> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getBlockedChannels("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushBlockedChannel(channel: SupabaseBlockedChannel) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertBlockedChannel(channel = listOf(channel))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteBlockedChannel(id: Long, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteBlockedChannel("eq.$id", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Allowed Channels Pull / Push / Delete
    suspend fun fetchAllowedChannels(email: String): List<SupabaseAllowedChannel> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getAllowedChannels("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushAllowedChannel(channel: SupabaseAllowedChannel) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertAllowedChannel(channel = listOf(channel))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAllowedChannel(id: Long, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteAllowedChannel("eq.$id", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Playlists Pull / Push / Delete
    suspend fun fetchPlaylists(email: String): List<SupabasePlaylist> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getPlaylists("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushPlaylist(playlist: SupabasePlaylist) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertPlaylist(playlist = listOf(playlist))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePlaylist(id: Long, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deletePlaylist("eq.$id", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Playlist Videos Pull / Push / Delete
    suspend fun fetchPlaylistVideos(email: String): List<SupabasePlaylistVideo> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getPlaylistVideos("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushPlaylistVideo(video: SupabasePlaylistVideo) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertPlaylistVideo(video = listOf(video))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePlaylistVideo(playlistId: Long, videoId: String, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deletePlaylistVideo("eq.$playlistId", "eq.$videoId", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Favorites Pull / Push / Delete
    suspend fun fetchFavorites(email: String): List<SupabaseFavorite> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getFavorites("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushFavorite(favorite: SupabaseFavorite) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertFavorite(favorite = listOf(favorite))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteFavorite(profileId: Long, videoId: String, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteFavorite("eq.$profileId", "eq.$videoId", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // History Pull / Push / Delete
    suspend fun fetchHistory(email: String): List<SupabaseHistory> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getHistory("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushHistory(history: SupabaseHistory) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertHistory(history = listOf(history))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteHistory(profileId: Long, email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteHistory("eq.$profileId", "eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Search History / Attempts Pull / Push / Delete
    suspend fun fetchSearchAttempts(email: String): List<SupabaseSearchAttempt> = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext emptyList()
        try {
            client.getSearchAttempts("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun pushSearchAttempt(attempt: SupabaseSearchAttempt) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.upsertSearchAttempt(attempt = listOf(attempt))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSearchAttempts(email: String) = withContext(Dispatchers.IO) {
        val client = api ?: return@withContext
        try {
            client.deleteSearchAttempts("eq.$email")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
