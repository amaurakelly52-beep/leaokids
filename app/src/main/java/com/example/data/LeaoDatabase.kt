package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities for Leão Kids ---

@Entity(tableName = "active_session")
data class ActiveSession(
    @PrimaryKey val id: Long = 1,
    val activeEmail: String = "visitor"
)

@Entity(tableName = "children_profiles")
data class ChildProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBoy: Boolean, // True for Boy (Cosmic), False for Girl (Magic)
    val avatarUrl: String,
    val creationTime: Long = System.currentTimeMillis(),
    val parentEmail: String = "visitor"
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val parentEmail: String = "visitor"
)

@Entity(tableName = "playlist_videos")
data class PlaylistVideo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val parentEmail: String = "visitor"
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parentEmail: String = "visitor"
)

@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val watchedDurationSeconds: Long,
    val parentEmail: String = "visitor"
)

@Entity(tableName = "blocked_words")
data class BlockedWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val parentEmail: String = "visitor"
)

@Entity(tableName = "blocked_channels")
data class BlockedChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String,
    val parentEmail: String = "visitor"
)

@Entity(tableName = "allowed_channels")
data class AllowedChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String,
    val parentEmail: String = "visitor"
)

@Entity(tableName = "search_history")
data class BlockedSearchAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parentEmail: String = "visitor"
)

@Entity(tableName = "parent_config")
data class ParentConfig(
    @PrimaryKey val connectedEmail: String = "visitor", // Keyed by email
    val pinCode: String = "1234",
    val screenTimeLimitMinutes: Int = 60,
    val isStrictChannelMode: Boolean = false, // If true, only allow AllowedChannels list
    val isSmartCuratorMode: Boolean = false, // If true, filter with smart Gemini AI prompt
    val connectedName: String? = null,
    val connectedPhoto: String? = null
)

@Entity(tableName = "approved_videos", primaryKeys = ["id", "parentEmail"])
data class ApprovedVideo(
    val id: String,
    val parentEmail: String = "visitor",
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationText: String,
    val category: String,
    val description: String
)

// --- DAOs (Data Access Objects) ---

@Dao
interface LeaoDao {

    // Active session management
    @Query("SELECT * FROM active_session WHERE id = 1 LIMIT 1")
    suspend fun getActiveSession(): ActiveSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveSession(session: ActiveSession)

    // Children profiles
    @Query("SELECT * FROM children_profiles WHERE parentEmail = :email ORDER BY creationTime ASC")
    fun getAllProfiles(email: String): Flow<List<ChildProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ChildProfile): Long

    @Query("DELETE FROM children_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: Long)

    @Query("SELECT * FROM children_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ChildProfile?

    // Playlists
    @Query("SELECT * FROM playlists WHERE profileId = :profileId AND parentEmail = :email")
    fun getPlaylistsByProfile(profileId: Long, email: String): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // Playlist Videos
    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId AND parentEmail = :email")
    fun getVideosForPlaylist(playlistId: Long, email: String): Flow<List<PlaylistVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideo(video: PlaylistVideo)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId AND parentEmail = :email")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String, email: String)

    // Favorites
    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND parentEmail = :email ORDER BY timestamp DESC")
    fun getFavoritesForProfile(profileId: Long, email: String): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND videoId = :videoId AND parentEmail = :email")
    suspend fun removeFavorite(profileId: Long, videoId: String, email: String)

    @Query("SELECT count(*) FROM favorites WHERE profileId = :profileId AND videoId = :videoId AND parentEmail = :email")
    suspend fun isFavorite(profileId: Long, videoId: String, email: String): Int

    // History
    @Query("SELECT * FROM history WHERE profileId = :profileId AND parentEmail = :email ORDER BY timestamp DESC")
    fun getHistoryForProfile(profileId: Long, email: String): Flow<List<History>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistoryEntry(history: History)

    @Query("DELETE FROM history WHERE profileId = :profileId AND parentEmail = :email")
    suspend fun clearHistory(profileId: Long, email: String)

    // Blocked words
    @Query("SELECT * FROM blocked_words WHERE parentEmail = :email ORDER BY word ASC")
    fun getBlockedWordsFlow(email: String): Flow<List<BlockedWord>>

    @Query("SELECT * FROM blocked_words WHERE parentEmail = :email")
    suspend fun getBlockedWordsList(email: String): List<BlockedWord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBlockedWord(blockedWord: BlockedWord)

    @Query("DELETE FROM blocked_words WHERE id = :id AND parentEmail = :email")
    suspend fun deleteBlockedWord(id: Long, email: String)

    // Blocked channels
    @Query("SELECT * FROM blocked_channels WHERE parentEmail = :email ORDER BY channelName ASC")
    fun getBlockedChannelsFlow(email: String): Flow<List<BlockedChannel>>

    @Query("SELECT * FROM blocked_channels WHERE parentEmail = :email")
    suspend fun getBlockedChannelsList(email: String): List<BlockedChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBlockedChannel(blockedChannel: BlockedChannel)

    @Query("DELETE FROM blocked_channels WHERE id = :id AND parentEmail = :email")
    suspend fun deleteBlockedChannel(id: Long, email: String)

    // Allowed channels
    @Query("SELECT * FROM allowed_channels WHERE parentEmail = :email ORDER BY channelName ASC")
    fun getAllowedChannelsFlow(email: String): Flow<List<AllowedChannel>>

    @Query("SELECT * FROM allowed_channels WHERE parentEmail = :email")
    suspend fun getAllowedChannelsList(email: String): List<AllowedChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllowedChannel(allowedChannel: AllowedChannel)

    @Query("DELETE FROM allowed_channels WHERE id = :id AND parentEmail = :email")
    suspend fun deleteAllowedChannel(id: Long, email: String)

    // Blocked Search Attempts
    @Query("SELECT * FROM search_history WHERE profileId = :profileId AND parentEmail = :email ORDER BY timestamp DESC")
    fun getBlockedSearchAttempts(profileId: Long, email: String): Flow<List<BlockedSearchAttempt>>

    @Query("SELECT * FROM search_history WHERE parentEmail = :email ORDER BY timestamp DESC")
    fun getAllBlockedSearchAttempts(email: String): Flow<List<BlockedSearchAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logBlockedSearchAttempt(attempt: BlockedSearchAttempt)

    @Query("DELETE FROM search_history WHERE parentEmail = :email")
    suspend fun clearBlockedSearchLog(email: String)

    // General Parent Configuration (Keyed by connectedEmail)
    @Query("SELECT * FROM parent_config WHERE connectedEmail = :email LIMIT 1")
    fun getParentConfigFlow(email: String): Flow<ParentConfig?>

    @Query("SELECT * FROM parent_config WHERE connectedEmail = :email LIMIT 1")
    suspend fun getParentConfig(email: String): ParentConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveParentConfig(config: ParentConfig)

    // Approved Videos
    @Query("SELECT * FROM approved_videos WHERE parentEmail = :email ORDER BY title ASC")
    fun getAllApprovedVideos(email: String): Flow<List<ApprovedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovedVideo(video: ApprovedVideo)

    @Query("DELETE FROM approved_videos WHERE id = :id AND parentEmail = :email")
    suspend fun deleteApprovedVideo(id: String, email: String)

    // Bulk Clear operations for cloud sync
    @Query("DELETE FROM children_profiles WHERE parentEmail = :email")
    suspend fun clearAllProfiles(email: String)

    @Query("DELETE FROM playlists WHERE parentEmail = :email")
    suspend fun clearAllPlaylists(email: String)

    @Query("DELETE FROM playlist_videos WHERE parentEmail = :email")
    suspend fun clearAllPlaylistVideos(email: String)

    @Query("DELETE FROM favorites WHERE parentEmail = :email")
    suspend fun clearAllFavorites(email: String)

    @Query("DELETE FROM history WHERE parentEmail = :email")
    suspend fun clearAllHistory(email: String)

    @Query("DELETE FROM blocked_words WHERE parentEmail = :email")
    suspend fun clearAllBlockedWords(email: String)

    @Query("DELETE FROM blocked_channels WHERE parentEmail = :email")
    suspend fun clearAllBlockedChannels(email: String)

    @Query("DELETE FROM allowed_channels WHERE parentEmail = :email")
    suspend fun clearAllAllowedChannels(email: String)

    @Query("DELETE FROM search_history WHERE parentEmail = :email")
    suspend fun clearAllSearchAttempts(email: String)

    @Query("DELETE FROM approved_videos WHERE parentEmail = :email")
    suspend fun clearAllApprovedVideos(email: String)

    // Bulk Insert operations for cloud sync
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ChildProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideos(playlistVideos: List<PlaylistVideo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<Favorite>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: List<History>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedWords(blockedWords: List<BlockedWord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedChannels(blockedChannels: List<BlockedChannel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllowedChannels(allowedChannels: List<AllowedChannel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchAttempts(attempts: List<BlockedSearchAttempt>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovedVideos(videos: List<ApprovedVideo>)
}

// --- App Database Class ---

@Database(
    entities = [
        ChildProfile::class,
        Playlist::class,
        PlaylistVideo::class,
        Favorite::class,
        History::class,
        BlockedWord::class,
        BlockedChannel::class,
        AllowedChannel::class,
        BlockedSearchAttempt::class,
        ParentConfig::class,
        ApprovedVideo::class,
        ActiveSession::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LeaoDatabase : RoomDatabase() {
    abstract fun dao(): LeaoDao

    companion object {
        @Volatile
        private var INSTANCE: LeaoDatabase? = null

        fun getDatabase(context: Context): LeaoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeaoDatabase::class.java,
                    "leao_kids_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
