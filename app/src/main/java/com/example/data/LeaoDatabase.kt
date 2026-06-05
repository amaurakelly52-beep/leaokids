package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities for Leão Kids ---

@Entity(tableName = "children_profiles")
data class ChildProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBoy: Boolean, // True for Boy (Cosmic), False for Girl (Magic)
    val avatarUrl: String,
    val creationTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String
)

@Entity(tableName = "playlist_videos")
data class PlaylistVideo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val timestamp: Long = System.currentTimeMillis()
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
    val watchedDurationSeconds: Long
)

@Entity(tableName = "blocked_words")
data class BlockedWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String
)

@Entity(tableName = "blocked_channels")
data class BlockedChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String
)

@Entity(tableName = "allowed_channels")
data class AllowedChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String
)

@Entity(tableName = "search_history")
data class BlockedSearchAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "parent_config")
data class ParentConfig(
    @PrimaryKey val id: Long = 1, // Single row configuration
    val pinCode: String = "1234",
    val screenTimeLimitMinutes: Int = 60,
    val isStrictChannelMode: Boolean = false, // If true, only allow AllowedChannels list
    val isSmartCuratorMode: Boolean = false, // If true, filter with smart Gemini AI prompt
    val connectedEmail: String? = null,
    val connectedName: String? = null,
    val connectedPhoto: String? = null
)

@Entity(tableName = "approved_videos")
data class ApprovedVideo(
    @PrimaryKey val id: String,
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

    // Children profiles
    @Query("SELECT * FROM children_profiles ORDER BY creationTime ASC")
    fun getAllProfiles(): Flow<List<ChildProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ChildProfile): Long

    @Query("DELETE FROM children_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: Long)

    @Query("SELECT * FROM children_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ChildProfile?

    // Playlists
    @Query("SELECT * FROM playlists WHERE profileId = :profileId")
    fun getPlaylistsByProfile(profileId: Long): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // Playlist Videos
    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId")
    fun getVideosForPlaylist(playlistId: Long): Flow<List<PlaylistVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideo(video: PlaylistVideo)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String)

    // Favorites
    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getFavoritesForProfile(profileId: Long): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND videoId = :videoId")
    suspend fun removeFavorite(profileId: Long, videoId: String)

    @Query("SELECT count(*) FROM favorites WHERE profileId = :profileId AND videoId = :videoId")
    suspend fun isFavorite(profileId: Long, videoId: String): Int

    // History
    @Query("SELECT * FROM history WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getHistoryForProfile(profileId: Long): Flow<List<History>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistoryEntry(history: History)

    @Query("DELETE FROM history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: Long)

    // Blocked words
    @Query("SELECT * FROM blocked_words ORDER BY word ASC")
    fun getBlockedWordsFlow(): Flow<List<BlockedWord>>

    @Query("SELECT * FROM blocked_words")
    suspend fun getBlockedWordsList(): List<BlockedWord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBlockedWord(blockedWord: BlockedWord)

    @Query("DELETE FROM blocked_words WHERE id = :id")
    suspend fun deleteBlockedWord(id: Long)

    // Blocked channels
    @Query("SELECT * FROM blocked_channels ORDER BY channelName ASC")
    fun getBlockedChannelsFlow(): Flow<List<BlockedChannel>>

    @Query("SELECT * FROM blocked_channels")
    suspend fun getBlockedChannelsList(): List<BlockedChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBlockedChannel(blockedChannel: BlockedChannel)

    @Query("DELETE FROM blocked_channels WHERE id = :id")
    suspend fun deleteBlockedChannel(id: Long)

    // Allowed channels
    @Query("SELECT * FROM allowed_channels ORDER BY channelName ASC")
    fun getAllowedChannelsFlow(): Flow<List<AllowedChannel>>

    @Query("SELECT * FROM allowed_channels")
    suspend fun getAllowedChannelsList(): List<AllowedChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllowedChannel(allowedChannel: AllowedChannel)

    @Query("DELETE FROM allowed_channels WHERE id = :id")
    suspend fun deleteAllowedChannel(id: Long)

    // Blocked Search Attempts
    @Query("SELECT * FROM search_history WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getBlockedSearchAttempts(profileId: Long): Flow<List<BlockedSearchAttempt>>

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getAllBlockedSearchAttempts(): Flow<List<BlockedSearchAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logBlockedSearchAttempt(attempt: BlockedSearchAttempt)

    @Query("DELETE FROM search_history")
    suspend fun clearBlockedSearchLog()

    // General Parent Configuration
    @Query("SELECT * FROM parent_config WHERE id = 1 LIMIT 1")
    fun getParentConfigFlow(): Flow<ParentConfig?>

    @Query("SELECT * FROM parent_config WHERE id = 1 LIMIT 1")
    suspend fun getParentConfig(): ParentConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveParentConfig(config: ParentConfig)

    // Approved Videos
    @Query("SELECT * FROM approved_videos ORDER BY title ASC")
    fun getAllApprovedVideos(): Flow<List<ApprovedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovedVideo(video: ApprovedVideo)

    @Query("DELETE FROM approved_videos WHERE id = :id")
    suspend fun deleteApprovedVideo(id: String)
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
        ApprovedVideo::class
    ],
    version = 2,
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
