package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

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
            id = "F2hc2R_SgKA",
            title = "Viagem pelo Sistema Solar",
            channelName = "NASA Kids",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCMujyXGZZ3qHY6Af3h5vGHWNA4wa4UGIgLCOGCwXWWMBrYGHFcTCDxMo1KG7JaPcWdk8qC4mk8nq5jbioeR8JSYJei0WU1rWzGTQNvHUAk5Do393nJGinaEuueNr3AwvP28q_Tx8bkUTMfsCf7MkdKG4fJNZpoO3wZLahi9tLlXJkia6-RUsQge4Yo3sYRXACxkjkTQsapmOPyHVKvSV-_eV9AS7kRKo0SWg4nj6CA-5XS-p7FHiNv56nAuqbxtjVheKH0rYy5vEU",
            durationText = "4:32",
            category = "Astronomia",
            description = "Uma linda viagem 3D para conhecer todos os planetas brilhantes do nosso Sistema Solar!"
        ),
        KidVideo(
            id = "v_2m3l-wXgQ",
            title = "Como os Foguetes Voam?",
            channelName = "Astronomia Animada",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA8dynhL5X3SwTCd9OpPPW2XMEb036gX0VnvwJetIxRaLMaBJrG2C4BP5JmjX9Dyqx8_-vsZw66sGLN-ENUTCmrWSR8l43ciGzaQFTMQ2PZF7qBid0CmbxgeN0lwBbFQ9t30rVt5h8KjqF7lZgcIihIX9UR_mmpEIRYVEBFGvApzrXtS15MkA6GCx0rQfnQWkpQIq-xcpfRyIm3qWmsdASYiqM8jwAuCCChO0_zoNQ0p4CIW_Xc1WmD-BhAUfVKocE-VtL8lW3jXhQ",
            durationText = "5:10",
            category = "Astronomia",
            description = "Descubra como os foguetes espaciais sobem tão alto até as estrelas usando ciência de verdade!"
        ),
        KidVideo(
            id = "m_S_7wYURlM",
            title = "Um Passeio na Lua",
            channelName = "Ciência das Crianças",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCWOl0c03g5asKS9YxG_FSXOjU2Gml2HRYbvriGGnvOXiGDWZN8GVRGvy1pxGTbteINlx123nFhj9r6_SSS66SrrZFhBT5NtexS3bVHn8ubnO32ulwyPAp1uYYLWJiuCVJAWDJXuiMXgMMRP0O9owghcIajOXbdVfC2h3NkPMupdOMxoAn0de0EeMk8XFOMhMFjCuj00M1wjdCozmAopbOVSE3HDk-OlmO_u4xYK7B0buWEMM2Q4UBLmE0XC5p-HuUMFqrVJlodzCM",
            durationText = "3:55",
            category = "Astronomia",
            description = "Venha descobrir o que os astronautas fazem quando pisam na superfície brilhante da nossa Lua."
        ),
        KidVideo(
            id = "b3WdfQLgHkk",
            title = "O Rei T-Rex",
            channelName = "Dino Aventura",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAnN38Vjb_iGJZvKuHF0ECkm__XwmrZOgc6dQuyqf2dyAwAQY183XXKgdngWohxfSeNI7jMeRYOaiB_QU33OqR0yeSdQTPIUzhuw_Fba0VGQUkatfAqdElSNyV0pxjj7mLdFSDbtCpOKvbED2Q64BCYnsWgGQEK-anWUTHK6xirwviN1uMqnDFIOPzyEhWfmYJdwUr_zjDgdLOHtp9d_4Z6GdqK9HyF4MrpKczJXpuKqKpYPYNvxMpwJS-ZrLfVfXHMZWNIwXxjbxQ",
            durationText = "4:45",
            category = "Dinossauros",
            description = "Uma aventura divertida pelas densas florestas de dinossauros para ver o gigante T-Rex comendo folhas e brincando!"
        ),
        KidVideo(
            id = "zWb4W048aAw",
            title = "Tricerátops Amigo",
            channelName = "Planeta Verde Kids",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDgp_V7ZYUouX7LTpoPDn4hXdUShSGbGqUKmCpXfvn0tj1BRr3ZnjnCIIoTOxR9knon-T7oaUcQd92cF0wmFA3ITfV6jGS67RK1Xu9_l0MZ9U0JUWq9rig7CG_Fi3h-e3-4_9I43fitJga3RwM6QHhodXrPBuO9spwYzEUvcof1SyHfzwhQRSyn6HL3Xd7qTzbmsQoINPtS4LsD60HReWmS3SuWgedk5RqzAqyyMJwiQtLGCdBETEPXcS44oBPdLpDcpHo1brG7uIE",
            durationText = "3:40",
            category = "Dinossauros",
            description = "Conheça o triceratops, um gigante fofo com chifres que adora comer plantas e passear nos campos floridos."
        ),
        KidVideo(
            id = "a2vD8389_aw",
            title = "Pterodáctilo Veloz",
            channelName = "Planeta Verde Kids",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB3EYUYQpqD2q0fZS2KJ6J7xlv58VUKOUveISyD2Xzbai57P1y2EtFEaC9rJ4Lc9vQvUK7OePJ2SEueh0O3R7WsHNan2-4Oc5p60o3gDGesJ4Z5IghZSMiVLvcTsQOBANv06zuA_pLQyhrgP9vfeM698i68eN7wLqXifpAZgf3aRSr0TliutlB7pWMvwqdAmyMjdzx8vPu8sJxQA1KAK9k6VtYcDB9k1V0qYjgVdfdY6yaiii9gXw_EihMbLeUkDTgsfczyUOE1nJA",
            durationText = "5:02",
            category = "Dinossauros",
            description = "Olhe para cima! O Pterodáctilo abre suas lindas asas e voa alto pelo céu cor de laranja!"
        ),
        KidVideo(
            id = "t3_aXv8a9_M",
            title = "Experimento do Vulcão Caseiro",
            channelName = "Manual do Mundo",
            thumbnailUrl = "https://images.unsplash.com/photo-1610484826967-09c5720778c7?q=80&w=600&auto=format&fit=crop",
            durationText = "8:20",
            category = "Ciências",
            description = "Aprenda a criar uma erupção de vulcão segura na sua cozinha usando vinagre e bicarbonato!"
        ),
        KidVideo(
            id = "vXY_8_X9WQw",
            title = "Como se Faz o Arco-Íris?",
            channelName = "Curiosos de Plantão",
            thumbnailUrl = "https://images.unsplash.com/photo-1548263544-24e0e7853669?q=80&w=600&auto=format&fit=crop",
            durationText = "4:12",
            category = "Ciências",
            description = "As gotinhas de chuva dividem a luz do sol em várias cores mágicas! Venha descobrir como."
        ),
        KidVideo(
            id = "b8vW9W9_aX8",
            title = "Aquarela Musical",
            channelName = "Galinha Pintadinha",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCsfhXa4o0IwpxUKW-JQ3LcL2Hb1rlUV5aQ-dXVx6hFg38WS_nXuDfli-rtKxA7jIVq7gRUxqvtGVgiSkiCjo3LjG3M4QpdoN4ODB4eP25U8XOyrh-BwjvUPT0SCVb7bpSpU-dqYpIkOhAvryQe5vY4VQ6Ony-DbPH1XcvIImwAUtDCNw5TN0YbeUP12CUwO4uXqB5qdh5b8kIoAAIAHN2Z0_XgtnAxPCJ94y4Cda6aeRkF8fZvIMve_5zYD5JrtXOj-uGx_SPIWos",
            durationText = "4:25",
            category = "Música",
            description = "Uma linda e educativa canção infantil ensinando as notas musicais com a diversão das cores."
        ),
        KidVideo(
            id = "vW98Xa8X_WW",
            title = "Amigos da Floresta",
            channelName = "Mundo Bita",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBNaoKOMysePMQnp40QEwIbKh9DBYob5h4X3dZrvOWJrR8ZVcQPN9roSsZpbFoHJI0Qxi0G94AywnpW84cDkYyl0S0hASPG9NuMsZf7047hlT1WqQ2pn6pmlV8LY_TuWncFDIbGcaAzDCIm9iy-MjEsoQ3SkAeo6_dYrWxfb0H-XlvJauw8BUOxZPhvp9VGozeq_8GE3OVoiM7zdKr_Pu_-b1glPU6f2B050-CqxgFEgYzhFQBZZkCQ0n2pj2a14s-ww2-L_SGUhhI",
            durationText = "3:40",
            category = "Música",
            description = "Conheça os animais bebês mais fofos e cante em ritmo alegre das descobertas na natureza."
        ),
        KidVideo(
            id = "x89_vXZwQ8W",
            title = "Pintando Estrelas",
            channelName = "Artes das Crianças",
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBMM8fRALh9qRaYFlNQxR8LIDyc_PNZ2C0nYgxrqL0Ld4wW1aQJLAGGWDh2n9XJvEbVFZdvOBHoxWqNt-lrakdMMkRidajYy4xPgxHsLpy4Bwb_1Yy2sDhU8G8c09qxGK9ERnFRoumb7j47424TEeUb8OrQPHpgN3DrRhB_yt9cBwFCsJ5DPslXpJB4yTZhv8gyt5U6xvfZlaQZ3YbphsgT_52pZl4c_Az8XJReb1IskgAIO3_WS52Yx0tHAb3tqaPQIwRrFsdzUt0",
            durationText = "6:10",
            category = "Artes",
            description = "Pegue seu pincel! Vamos aprender a desenhar estrelas brilhantes no papel com tintas super cintilantes."
        )
    )

    // --- Profiles ---
    val profiles: Flow<List<ChildProfile>> = dao.getAllProfiles()

    suspend fun createProfile(name: String, isBoy: Boolean, avatarUrl: String): Long {
        return dao.insertProfile(ChildProfile(name = name, isBoy = isBoy, avatarUrl = avatarUrl))
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

        // 4. Preload parent config
        val config = dao.getParentConfig()
        if (config == null) {
            dao.saveParentConfig(ParentConfig(pinCode = "1234", screenTimeLimitMinutes = 60))
        }

        // 5. Preload default children profiles if absolutely empty
        val list = dao.getAllProfiles().firstOrNull() ?: emptyList()
        if (list.isEmpty()) {
            dao.insertProfile(
                ChildProfile(
                    name = "Léo",
                    isBoy = true,
                    avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCdXv6aunqcY7UKJnaAGtqyXESXavtwtHnAf9EmU1fqyiHEatwzRGybdOE1L1NGKzckVLeqqYKG3elQDIDitkkJYKjnxFhKqI0HKHRbmH2Ccmr_sDUxgk1yQ3kW36S23gj_zhBM8A2chuYSWGFBokQ7AwO93LcMQAasXPJds_srS1MCOLorwaLFOkWyCsZGxN-pAUFjoZ9XMZLfYKGMknOVs22rAwJ0pFdLFcfGfxzLlSNIe57FjYpQw2Ht80sbPe6mXVq0YNjh-Z8"
                )
            )
            dao.insertProfile(
                ChildProfile(
                    name = "Bia",
                    isBoy = false,
                    avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC7ugoftgcrSMmMHtclryDr_ufH5WJjO05gxQ6HjaSwEVBbek91fqUH6i0y1n6YGSqoc1CmM7O2kB4d1ogx9vljivVCv1I9MBYj7OchX3B9LnZVIDBHwnKoimbdJcPHEqf8CfZWpiXi_JlhpmLPsWHZTv8ORU4Ym73ZI3dtZC95O-BKdRDSvuQwT2teQRkt5qNyrBZc-NYxdq-Muos4Z9pS7p6lsV7hcq0Uw5OYRjGm1zLn2pRD6JRVTt0I-AqhwoDLQDHhcT3YvL8"
                )
            )
        }
    }
}
