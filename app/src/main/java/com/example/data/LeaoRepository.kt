package com.example.data

import kotlinx.coroutines.flow.Flow
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
    val presetVideos = listOf(
        KidVideo(
            id = "wX_347_bY9U",
            title = "Música dos Planetas Animados",
            channelName = "Mundo Bita",
            thumbnailUrl = "https://img.youtube.com/vi/wX_347_bY9U/hqdefault.jpg",
            durationText = "3:20",
            category = "Música",
            description = "Cante e dance com o Bita numa jornada de foguete pelos astros mais brilhantes!"
        ),
        KidVideo(
            id = "k9vL_XmpSqE",
            title = "Como Funciona o Arco-Íris?",
            channelName = "Manual do Mundo",
            thumbnailUrl = "https://img.youtube.com/vi/k9vL_XmpSqE/hqdefault.jpg",
            durationText = "6:40",
            category = "Ciências",
            description = "Iberê explica com experimentos simples como a luz do sol vira cores mágicas na água!"
        ),
        KidVideo(
            id = "vP79aL_KxpM",
            title = "O Mistério das Estrelas Cadentes",
            channelName = "Ciência Espacial",
            thumbnailUrl = "https://img.youtube.com/vi/vP79aL_KxpM/hqdefault.jpg",
            durationText = "5:45",
            category = "Astronomia",
            description = "Descubra o que realmente são as estrelas cadentes que brilham à noite no céu."
        ),
        KidVideo(
            id = "bB3_F9Lp_Kx",
            title = "Galinha Pintadinha - Noite de São João",
            channelName = "Galinha Pintadinha",
            thumbnailUrl = "https://img.youtube.com/vi/bB3_F9Lp_Kx/hqdefault.jpg",
            durationText = "2:50",
            category = "Música",
            description = "As canções e danças mais alegres da galinha mais amada do Brasil!"
        ),
        KidVideo(
            id = "dD8_Xpl_Lka",
            title = "Peppa Pig - Visita ao Planetário",
            channelName = "Peppa Pig Português",
            thumbnailUrl = "https://img.youtube.com/vi/dD8_Xpl_Lka/hqdefault.jpg",
            durationText = "5:00",
            category = "Geral",
            description = "Peppa, George e seus amiguinhos vão ao planetário aprender sobre as estrelas e a lua."
        ),
        KidVideo(
            id = "2pHdf0X9mUY",
            title = "Como é Feito o Vidro?",
            channelName = "Manual do Mundo",
            thumbnailUrl = "https://img.youtube.com/vi/2pHdf0X9mUY/hqdefault.jpg",
            durationText = "8:15",
            category = "Ciências",
            description = "Conheça todo o processo industrial e a ciência por trás da fabricação do vidro!"
        ),
        KidVideo(
            id = "3X1G_v2j13M",
            title = "Viajar Pelo Safari",
            channelName = "Mundo Bita",
            thumbnailUrl = "https://img.youtube.com/vi/3X1G_v2j13M/hqdefault.jpg",
            durationText = "3:10",
            category = "Música",
            description = "Conheça os animais do safari com o Bita nesta canção super divertida!"
        )
    )

    // --- Safe YouTube Premium simulated global pool ---
    val onlineYoutubeVideos = emptyList<KidVideo>()

    // --- Cloud Synchronization Pull ---
    suspend fun syncFromCloud(email: String) {
        if (email == "visitor" || !SupabaseService.isConfigured()) return
        try {
            // 1. Fetch remote data from Supabase
            val remoteProfiles = SupabaseService.fetchProfiles(email)
            val remoteConfig = SupabaseService.fetchConfig(email)
            val remoteApproved = SupabaseService.fetchApprovedVideos(email)
            val remoteWords = SupabaseService.fetchBlockedWords(email)
            val remoteBlockedCh = SupabaseService.fetchBlockedChannels(email)
            val remoteAllowedCh = SupabaseService.fetchAllowedChannels(email)
            val remotePlaylists = SupabaseService.fetchPlaylists(email)
            val remotePlaylistVideos = SupabaseService.fetchPlaylistVideos(email)
            val remoteFavorites = SupabaseService.fetchFavorites(email)
            val remoteHistory = SupabaseService.fetchHistory(email)
            val remoteSearchAttempts = SupabaseService.fetchSearchAttempts(email)

            // 2. Clear local Room database tables for this specific email
            dao.clearAllProfiles(email)
            dao.clearAllPlaylists(email)
            dao.clearAllPlaylistVideos(email)
            dao.clearAllFavorites(email)
            dao.clearAllHistory(email)
            dao.clearAllBlockedWords(email)
            dao.clearAllBlockedChannels(email)
            dao.clearAllAllowedChannels(email)
            dao.clearAllSearchAttempts(email)
            dao.clearAllApprovedVideos(email)

            // 3. Bulk insert the fetched remote records back to local Room database
            dao.insertProfiles(remoteProfiles.map {
                ChildProfile(
                    id = it.id,
                    name = it.name,
                    isBoy = it.isBoy,
                    avatarUrl = it.avatarUrl,
                    creationTime = it.creationTime,
                    parentEmail = it.parentEmail,
                    screenTimeLimitMinutes = it.screenTimeLimitMinutes,
                    isStrictChannelMode = it.isStrictChannelMode,
                    isSmartCuratorMode = it.isSmartCuratorMode
                )
            })

            if (remoteConfig != null) {
                dao.saveParentConfig(
                    ParentConfig(
                        connectedEmail = remoteConfig.connectedEmail,
                        pinCode = remoteConfig.pinCode,
                        connectedName = remoteConfig.connectedName,
                        connectedPhoto = remoteConfig.connectedPhoto
                    )
                )
            }

            dao.insertApprovedVideos(remoteApproved.map {
                ApprovedVideo(
                    id = it.id,
                    profileId = it.profileId,
                    parentEmail = it.parentEmail,
                    title = it.title,
                    channelName = it.channelName,
                    thumbnailUrl = it.thumbnailUrl,
                    durationText = it.durationText,
                    category = it.category,
                    description = it.description
                )
            })

            dao.insertBlockedWords(remoteWords.map {
                BlockedWord(
                    id = it.id,
                    word = it.word,
                    profileId = it.profileId,
                    parentEmail = it.parentEmail
                )
            })

            dao.insertBlockedChannels(remoteBlockedCh.map {
                BlockedChannel(
                    id = it.id,
                    channelName = it.channelName,
                    profileId = it.profileId,
                    parentEmail = it.parentEmail
                )
            })

            dao.insertAllowedChannels(remoteAllowedCh.map {
                AllowedChannel(
                    id = it.id,
                    channelName = it.channelName,
                    profileId = it.profileId,
                    parentEmail = it.parentEmail
                )
            })

            dao.insertPlaylists(remotePlaylists.map {
                Playlist(
                    id = it.id,
                    profileId = it.profileId,
                    name = it.name,
                    parentEmail = it.parentEmail
                )
            })

            dao.insertPlaylistVideos(remotePlaylistVideos.map {
                PlaylistVideo(
                    id = it.id,
                    playlistId = it.playlistId,
                    videoId = it.videoId,
                    title = it.title,
                    channelName = it.channelName,
                    thumbnailUrl = it.thumbnailUrl,
                    parentEmail = it.parentEmail
                )
            })

            dao.insertFavorites(remoteFavorites.map {
                Favorite(
                    id = it.id,
                    profileId = it.profileId,
                    videoId = it.videoId,
                    title = it.title,
                    channelName = it.channelName,
                    thumbnailUrl = it.thumbnailUrl,
                    timestamp = it.timestamp,
                    parentEmail = it.parentEmail
                )
            })

            dao.insertHistory(remoteHistory.map {
                History(
                    id = it.id,
                    profileId = it.profileId,
                    videoId = it.videoId,
                    title = it.title,
                    channelName = it.channelName,
                    thumbnailUrl = it.thumbnailUrl,
                    timestamp = it.timestamp,
                    watchedDurationSeconds = it.watchedDurationSeconds,
                    parentEmail = it.parentEmail
                )
            })

            dao.insertSearchAttempts(remoteSearchAttempts.map {
                BlockedSearchAttempt(
                    id = it.id,
                    profileId = it.profileId,
                    query = it.query,
                    timestamp = it.timestamp,
                    parentEmail = it.parentEmail
                )
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Approved Videos CRUD ---
    fun getApprovedVideos(profileId: Long, email: String): Flow<List<KidVideo>> {
        return dao.getAllApprovedVideos(profileId, email).map { list ->
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

    suspend fun saveApprovedVideo(profileId: Long, video: KidVideo, email: String) {
        dao.insertApprovedVideo(
            ApprovedVideo(
                id = video.id,
                profileId = profileId,
                parentEmail = email,
                title = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                durationText = video.durationText,
                category = video.category,
                description = video.description
            )
        )
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushApprovedVideo(
                    SupabaseApprovedVideo(
                        id = video.id,
                        profileId = profileId,
                        parentEmail = email,
                        title = video.title,
                        channelName = video.channelName,
                        thumbnailUrl = video.thumbnailUrl,
                        durationText = video.durationText,
                        category = video.category,
                        description = video.description
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteApprovedVideo(id: String, profileId: Long, email: String) {
        dao.deleteApprovedVideo(id, profileId, email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deleteApprovedVideo(id, profileId, email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Profiles ---
    fun getProfiles(email: String): Flow<List<ChildProfile>> = dao.getAllProfiles(email)

    suspend fun createProfile(name: String, isBoy: Boolean, avatarUrl: String, email: String): Long {
        val id = dao.insertProfile(
            ChildProfile(
                name = name,
                isBoy = isBoy,
                avatarUrl = avatarUrl,
                parentEmail = email,
                screenTimeLimitMinutes = 60,
                isStrictChannelMode = false,
                isSmartCuratorMode = false
            )
        )
        autoPopulateDefaultsForProfile(id, email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushProfile(
                    SupabaseProfile(
                        id = id,
                        name = name,
                        isBoy = isBoy,
                        avatarUrl = avatarUrl,
                        creationTime = System.currentTimeMillis(),
                        parentEmail = email,
                        screenTimeLimitMinutes = 60,
                        isStrictChannelMode = false,
                        isSmartCuratorMode = false
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return id
    }

    suspend fun updateProfile(profile: ChildProfile) {
        dao.insertProfile(profile)
        if (SupabaseService.isConfigured() && profile.parentEmail != "visitor") {
            try {
                SupabaseService.pushProfile(
                    SupabaseProfile(
                        id = profile.id,
                        name = profile.name,
                        isBoy = profile.isBoy,
                        avatarUrl = profile.avatarUrl,
                        creationTime = profile.creationTime,
                        parentEmail = profile.parentEmail,
                        screenTimeLimitMinutes = profile.screenTimeLimitMinutes,
                        isStrictChannelMode = profile.isStrictChannelMode,
                        isSmartCuratorMode = profile.isSmartCuratorMode
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getProfile(id: Long): ChildProfile? {
        return dao.getProfileById(id)
    }

    suspend fun removeProfile(id: Long) {
        val profile = dao.getProfileById(id)
        dao.deleteProfile(id)
        if (profile != null && SupabaseService.isConfigured() && profile.parentEmail != "visitor") {
            try {
                SupabaseService.deleteProfile(id, profile.parentEmail)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- History ---
    fun getHistory(profileId: Long, email: String): Flow<List<History>> = dao.getHistoryForProfile(profileId, email)

    suspend fun registerWatchHistory(profileId: Long, video: KidVideo, secondsPlayed: Long, email: String) {
        val entry = History(
            profileId = profileId,
            videoId = video.id,
            title = video.title,
            channelName = video.channelName,
            thumbnailUrl = video.thumbnailUrl,
            watchedDurationSeconds = secondsPlayed,
            parentEmail = email
        )
        dao.addHistoryEntry(entry)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushHistory(
                    SupabaseHistory(
                        id = entry.id,
                        profileId = entry.profileId,
                        videoId = entry.videoId,
                        title = entry.title,
                        channelName = entry.channelName,
                        thumbnailUrl = entry.thumbnailUrl,
                        timestamp = entry.timestamp,
                        watchedDurationSeconds = entry.watchedDurationSeconds,
                        parentEmail = entry.parentEmail
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun clearHistory(profileId: Long, email: String) {
        dao.clearHistory(profileId, email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deleteHistory(profileId, email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Favorites ---
    fun getFavorites(profileId: Long, email: String): Flow<List<Favorite>> = dao.getFavoritesForProfile(profileId, email)

    suspend fun toggleFavorite(profileId: Long, video: KidVideo, email: String) {
        val isFav = dao.isFavorite(profileId, video.id, email) > 0
        if (isFav) {
            dao.removeFavorite(profileId, video.id, email)
            if (SupabaseService.isConfigured() && email != "visitor") {
                try {
                    SupabaseService.deleteFavorite(profileId, video.id, email)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            val fav = Favorite(
                profileId = profileId,
                videoId = video.id,
                title = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                parentEmail = email
            )
            dao.addFavorite(fav)
            if (SupabaseService.isConfigured() && email != "visitor") {
                try {
                    SupabaseService.pushFavorite(
                        SupabaseFavorite(
                            id = fav.id,
                            profileId = fav.profileId,
                            videoId = fav.videoId,
                            title = fav.title,
                            channelName = fav.channelName,
                            thumbnailUrl = fav.thumbnailUrl,
                            timestamp = fav.timestamp,
                            parentEmail = fav.parentEmail
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun isFavoriteVideo(profileId: Long, videoId: String, email: String): Boolean {
        return dao.isFavorite(profileId, videoId, email) > 0
    }

    // --- Playlists ---
    fun getPlaylists(profileId: Long, email: String): Flow<List<Playlist>> = dao.getPlaylistsByProfile(profileId, email)

    fun getPlaylistVideos(playlistId: Long, email: String): Flow<List<PlaylistVideo>> = dao.getVideosForPlaylist(playlistId, email)

    suspend fun createPlaylist(profileId: Long, name: String, email: String): Long {
        val id = dao.insertPlaylist(Playlist(profileId = profileId, name = name, parentEmail = email))
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushPlaylist(
                    SupabasePlaylist(
                        id = id,
                        profileId = profileId,
                        name = name,
                        parentEmail = email
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return id
    }

    suspend fun deletePlaylist(playlistId: Long, email: String) {
        dao.deletePlaylist(playlistId)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deletePlaylist(playlistId, email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: KidVideo, email: String) {
        val entry = PlaylistVideo(
            playlistId = playlistId,
            videoId = video.id,
            title = video.title,
            channelName = video.channelName,
            thumbnailUrl = video.thumbnailUrl,
            parentEmail = email
        )
        dao.insertPlaylistVideo(entry)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushPlaylistVideo(
                    SupabasePlaylistVideo(
                        id = entry.id,
                        playlistId = playlistId,
                        videoId = video.id,
                        title = video.title,
                        channelName = video.channelName,
                        thumbnailUrl = video.thumbnailUrl,
                        parentEmail = email
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String, email: String) {
        dao.removeVideoFromPlaylist(playlistId, videoId, email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deletePlaylistVideo(playlistId, videoId, email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Core parental rule lists ---
    fun getBlockedWords(profileId: Long, email: String): Flow<List<BlockedWord>> = dao.getBlockedWordsFlow(profileId, email)
    fun getBlockedChannels(profileId: Long, email: String): Flow<List<BlockedChannel>> = dao.getBlockedChannelsFlow(profileId, email)
    fun getAllowedChannels(profileId: Long, email: String): Flow<List<AllowedChannel>> = dao.getAllowedChannelsFlow(profileId, email)
    fun getBlockedSearchAttempts(profileId: Long, email: String): Flow<List<BlockedSearchAttempt>> = dao.getBlockedSearchAttempts(profileId, email)

    suspend fun addBlockedWord(profileId: Long, word: String, email: String) {
        val entry = BlockedWord(word = word.trim(), profileId = profileId, parentEmail = email)
        dao.addBlockedWord(entry)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushBlockedWord(
                    SupabaseBlockedWord(
                        id = entry.id,
                        word = entry.word,
                        profileId = profileId,
                        parentEmail = email
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeBlockedWord(id: Long, email: String) {
        dao.deleteBlockedWord(id, email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deleteBlockedWord(id, email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addBlockedChannel(profileId: Long, channel: String, email: String) {
        val entry = BlockedChannel(channelName = channel.trim(), profileId = profileId, parentEmail = email)
        dao.addBlockedChannel(entry)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushBlockedChannel(
                    SupabaseBlockedChannel(
                        id = entry.id,
                        channelName = entry.channelName,
                        profileId = profileId,
                        parentEmail = email
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeBlockedChannel(id: Long, email: String) {
        dao.deleteBlockedChannel(id, email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deleteBlockedChannel(id, email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addAllowedChannel(profileId: Long, channel: String, email: String) {
        val entry = AllowedChannel(channelName = channel.trim(), profileId = profileId, parentEmail = email)
        dao.addAllowedChannel(entry)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushAllowedChannel(
                    SupabaseAllowedChannel(
                        id = entry.id,
                        channelName = entry.channelName,
                        profileId = profileId,
                        parentEmail = email
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeAllowedChannel(id: Long, email: String) {
        dao.deleteAllowedChannel(id, email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deleteAllowedChannel(id, email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun logSearchBlock(profileId: Long, query: String, email: String) {
        val entry = BlockedSearchAttempt(profileId = profileId, query = query, parentEmail = email)
        dao.logBlockedSearchAttempt(entry)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.pushSearchAttempt(
                    SupabaseSearchAttempt(
                        id = entry.id,
                        profileId = profileId,
                        query = query,
                        timestamp = entry.timestamp,
                        parentEmail = email
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun clearBlockedLogs(email: String) {
        dao.clearBlockedSearchLog(email)
        if (SupabaseService.isConfigured() && email != "visitor") {
            try {
                SupabaseService.deleteSearchAttempts(email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Parent Configs ---
    fun getParentConfig(email: String): Flow<ParentConfig?> = dao.getParentConfigFlow(email)

    suspend fun getParentConfigDirect(email: String): ParentConfig? = dao.getParentConfig(email)

    suspend fun saveConfig(config: ParentConfig) {
        dao.saveParentConfig(config)
        if (SupabaseService.isConfigured() && config.connectedEmail != "visitor") {
            try {
                SupabaseService.pushConfig(
                    SupabaseConfig(
                        connectedEmail = config.connectedEmail,
                        pinCode = config.pinCode,
                        connectedName = config.connectedName,
                        connectedPhoto = config.connectedPhoto
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Active Session ---
    suspend fun getActiveEmail(): String {
        return dao.getActiveSession()?.activeEmail ?: "visitor"
    }

    suspend fun saveActiveEmail(email: String) {
        dao.saveActiveSession(ActiveSession(activeEmail = email))
    }

    // --- Startup Helper: Auto Populate defaults on launch if empty ---
    suspend fun autoPopulateDefaults(email: String) {
        // Preload parent config (default settings, no email/name)
        val config = dao.getParentConfig(email)
        if (config == null) {
            saveConfig(
                ParentConfig(
                    connectedEmail = email,
                    pinCode = "1234",
                    connectedName = null,
                    connectedPhoto = null
                )
            )
        }
    }

    suspend fun autoPopulateDefaultsForProfile(profileId: Long, email: String) {
        // 1. Preload defaults keywords
        val words = dao.getBlockedWordsList(profileId, email)
        if (words.isEmpty()) {
            val defaults = listOf("Luta", "Horror", "Assustador", "Morte", "Sangue", "Violência", "Terror", "Arma", "Monstro", "Crime", "Demônio")
            defaults.forEach { addBlockedWord(profileId, it, email) }
        }

        // 2. Preload blocked channels
        val channels = dao.getBlockedChannelsList(profileId, email)
        if (channels.isEmpty()) {
            val defaults = listOf("Canal XYZ", "Monster Mash", "Game Pro Kids 12+")
            defaults.forEach { addBlockedChannel(profileId, it, email) }
        }

        // 3. Preload allowed channels
        val allowed = dao.getAllowedChannelsList(profileId, email)
        if (allowed.isEmpty()) {
            val defaults = listOf("Galinha Pintadinha", "Manual do Mundo", "Mundo Bita", "NASA", "National Geographic Kids")
            defaults.forEach { addAllowedChannel(profileId, it, email) }
        }
    }
}
