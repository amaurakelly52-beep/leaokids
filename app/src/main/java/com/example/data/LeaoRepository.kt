package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// --- Video Data Struct ---
data class KidVideo(
    val id: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationText: String,
    val category: String,
    val description: String
)

class LeaoRepository(private val dao: LeaoDao) {

    // --- Curated Preset Children Videos ---
    val presetVideos = emptyList<KidVideo>()

    // --- Safe YouTube Premium simulated global pool ---
    val onlineYoutubeVideos = emptyList<KidVideo>()

    // --- Approved Videos CRUD ---
    fun getApprovedVideos(): Flow<List<KidVideo>> {
        return dao.getAllApprovedVideos().map { list ->
            list.map { entity ->
                KidVideo(
                    id = entity.id,
                    title = entity.title,
                    channelName = entity.channelName,
                    thumbnailUrl = entity.thumbnailUrl,
                    durationText = entity.durationText,
                    category = entity.category,
                    description = entity.description
                )
            }
        }
    }

    suspend fun saveApprovedVideo(video: KidVideo) {
        dao.insertApprovedVideo(
            ApprovedVideo(
                id = video.id,
                title = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                durationText = video.durationText,
                category = video.category,
                description = video.description
            )
        )
    }

    suspend fun deleteApprovedVideo(id: String) {
        dao.deleteApprovedVideo(id)
    }

    // --- Profiles ---
    val profiles: Flow<List<ChildProfile>> = dao.getAllProfiles()

    suspend fun createProfile(name: String, isBoy: Boolean, avatarUrl: String): Long {
        return dao.insertProfile(ChildProfile(name = name, isBoy = isBoy, avatarUrl = avatarUrl))
    }

    suspend fun updateProfile(profile: ChildProfile) {
        dao.insertProfile(profile)
    }

    suspend fun getProfile(id: Long): ChildProfile? {
        return dao.getProfileById(id)
    }

    suspend fun removeProfile(id: Long) {
        dao.deleteProfile(id)
    }

    // --- History ---
    fun getHistory(profileId: Long): Flow<List<History>> = dao.getHistoryForProfile(profileId)

    suspend fun registerWatchHistory(profileId: Long, video: KidVideo, secondsPlayed: Long) {
        dao.addHistoryEntry(
            History(
                profileId = profileId,
                videoId = video.id,
                title = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                watchedDurationSeconds = secondsPlayed
            )
        )
    }

    suspend fun clearHistory(profileId: Long) {
        dao.clearHistory(profileId)
    }

    // --- Favorites ---
    fun getFavorites(profileId: Long): Flow<List<Favorite>> = dao.getFavoritesForProfile(profileId)

    suspend fun toggleFavorite(profileId: Long, video: KidVideo) {
        if (dao.isFavorite(profileId, video.id) > 0) {
            dao.removeFavorite(profileId, video.id)
        } else {
            dao.addFavorite(
                Favorite(
                    profileId = profileId,
                    videoId = video.id,
                    title = video.title,
                    channelName = video.channelName,
                    thumbnailUrl = video.thumbnailUrl
                )
            )
        }
    }

    suspend fun isFavoriteVideo(profileId: Long, videoId: String): Boolean {
        return dao.isFavorite(profileId, videoId) > 0
    }

    // --- Playlists ---
    fun getPlaylists(profileId: Long): Flow<List<Playlist>> = dao.getPlaylistsByProfile(profileId)

    fun getPlaylistVideos(playlistId: Long): Flow<List<PlaylistVideo>> = dao.getVideosForPlaylist(playlistId)

    suspend fun createPlaylist(profileId: Long, name: String): Long {
        return dao.insertPlaylist(Playlist(profileId = profileId, name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: KidVideo) {
        dao.insertPlaylistVideo(
            PlaylistVideo(
                playlistId = playlistId,
                videoId = video.id,
                title = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl
            )
        )
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String) {
        dao.removeVideoFromPlaylist(playlistId, videoId)
    }

    // --- Core parental rule lists ---
    val blockedWords: Flow<List<BlockedWord>> = dao.getBlockedWordsFlow()
    val blockedChannels: Flow<List<BlockedChannel>> = dao.getBlockedChannelsFlow()
    val allowedChannels: Flow<List<AllowedChannel>> = dao.getAllowedChannelsFlow()
    val blockedSearchAttempts: Flow<List<BlockedSearchAttempt>> = dao.getAllBlockedSearchAttempts()

    suspend fun addBlockedWord(word: String) {
        dao.addBlockedWord(BlockedWord(word = word.trim()))
    }

    suspend fun removeBlockedWord(id: Long) {
        dao.deleteBlockedWord(id)
    }

    suspend fun addBlockedChannel(channel: String) {
        dao.addBlockedChannel(BlockedChannel(channelName = channel.trim()))
    }

    suspend fun removeBlockedChannel(id: Long) {
        dao.deleteBlockedChannel(id)
    }

    suspend fun addAllowedChannel(channel: String) {
        dao.addAllowedChannel(AllowedChannel(channelName = channel.trim()))
    }

    suspend fun removeAllowedChannel(id: Long) {
        dao.deleteAllowedChannel(id)
    }

    suspend fun logSearchBlock(profileId: Long, query: String) {
        dao.logBlockedSearchAttempt(BlockedSearchAttempt(profileId = profileId, query = query))
    }

    suspend fun clearBlockedLogs() {
        dao.clearBlockedSearchLog()
    }

    // --- Parent Configs ---
    val parentConfig: Flow<ParentConfig?> = dao.getParentConfigFlow()

    suspend fun saveConfig(config: ParentConfig) {
        dao.saveParentConfig(config)
    }

    // --- Startup Helper: Auto Populate defaults on launch if empty ---
    suspend fun autoPopulateDefaults() {
        // 1. Preload defaults keywords
        val words = dao.getBlockedWordsList()
        if (words.isEmpty()) {
            val defaults = listOf("Luta", "Horror", "Assustador", "Morte", "Sangue", "Violência", "Terror", "Arma", "Monstro", "Crime", "Demônio")
            defaults.forEach { dao.addBlockedWord(BlockedWord(word = it)) }
        }

        // 2. Preload blocked channels
        val channels = dao.getBlockedChannelsList()
        if (channels.isEmpty()) {
            val defaults = listOf("Canal XYZ", "Monster Mash", "Game Pro Kids 12+")
            defaults.forEach { dao.addBlockedChannel(BlockedChannel(channelName = it)) }
        }

        // 3. Preload allowed channels
        val allowed = dao.getAllowedChannelsList()
        if (allowed.isEmpty()) {
            val defaults = listOf("Galinha Pintadinha", "Manual do Mundo", "Mundo Bita", "NASA", "National Geographic Kids")
            defaults.forEach { dao.addAllowedChannel(AllowedChannel(channelName = it)) }
        }

        // 4. Preload parent config (default settings, no email/name)
        val config = dao.getParentConfig()
        if (config == null) {
            dao.saveParentConfig(
                ParentConfig(
                    pinCode = "1234",
                    screenTimeLimitMinutes = 60,
                    connectedEmail = null,
                    connectedName = null,
                    connectedPhoto = null
                )
            )
        }

        // 5. Preload default children profiles if absolutely empty (disabled to start clean)
        // val list = dao.getAllProfiles().firstOrNull() ?: emptyList()
    }
}
