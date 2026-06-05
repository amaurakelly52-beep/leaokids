package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Screen Navigation enum ---
enum class LeaoScreen {
    Splash,
    Login,
    ProfileSelection,
    ChildHome,
    Player,
    ParentGate,
    ParentDashboard,
    PianoGame,
    PuzzleGame,
    LimitsExceeded
}

class LeaoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LeaoDatabase.getDatabase(application)
    private val repository = LeaoRepository(database.dao())

    val presetVideos = repository.presetVideos

    // --- Navigation & Flow State ---
    private val _currentScreen = MutableStateFlow(LeaoScreen.Splash)
    val currentScreen: StateFlow<LeaoScreen> = _currentScreen.asStateFlow()

    private val _currentProfile = MutableStateFlow<ChildProfile?>(null)
    val currentProfile: StateFlow<ChildProfile?> = _currentProfile.asStateFlow()

    private val _activeVideo = MutableStateFlow<KidVideo?>(null)
    val activeVideo: StateFlow<KidVideo?> = _activeVideo.asStateFlow()

    private val _gateNextScreen = MutableStateFlow(LeaoScreen.ParentDashboard)
    val gateNextScreen: StateFlow<LeaoScreen> = _gateNextScreen.asStateFlow()

    // --- Parent Security Gate states ---
    val firstGateNum = MutableStateFlow(0)
    val secondGateNum = MutableStateFlow(0)
    val gateAnswerInput = MutableStateFlow("")
    val gateErrorMsg = MutableStateFlow("")

    // --- Search & Filter state ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<KidVideo>>(emptyList())
    val searchResults: StateFlow<List<KidVideo>> = _searchResults.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isSmartCurating = MutableStateFlow(false)
    val isSmartCurating: StateFlow<Boolean> = _isSmartCurating.asStateFlow()

    private val _unsafeSearchResponse = MutableStateFlow<ModerationResult?>(null)
    val unsafeSearchResponse: StateFlow<ModerationResult?> = _unsafeSearchResponse.asStateFlow()

    // --- Room Data Lists mapped to states ---
    val allProfiles: StateFlow<List<ChildProfile>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedWords: StateFlow<List<BlockedWord>> = repository.blockedWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedChannels: StateFlow<List<BlockedChannel>> = repository.blockedChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowedChannels: StateFlow<List<AllowedChannel>> = repository.allowedChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedSearchAttempts: StateFlow<List<BlockedSearchAttempt>> = repository.blockedSearchAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parentConfig: StateFlow<ParentConfig> = repository.parentConfig
        .map { it ?: ParentConfig() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ParentConfig())

    // --- Dynamics Child lists: Favorites, Playlists, History ---
    val favoritesList = MutableStateFlow<List<Favorite>>(emptyList())
    val playlistsList = MutableStateFlow<List<Playlist>>(emptyList())
    val historyList = MutableStateFlow<List<History>>(emptyList())

    // --- Direct Premium YouTube Curator State & Custom Approved list ---
    val customApprovedVideos: StateFlow<List<KidVideo>> = repository.getApprovedVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _parentYoutubeSearchQuery = MutableStateFlow("")
    val parentYoutubeSearchQuery: StateFlow<String> = _parentYoutubeSearchQuery.asStateFlow()

    private val _parentYoutubeSearchResults = MutableStateFlow<List<KidVideo>>(emptyList())
    val parentYoutubeSearchResults: StateFlow<List<KidVideo>> = _parentYoutubeSearchResults.asStateFlow()

    // Category Loading state and Youtube caching
    private val _isCategoryLoading = MutableStateFlow(false)
    val isCategoryLoading: StateFlow<Boolean> = _isCategoryLoading.asStateFlow()

    private val _categoryVideosCache = MutableStateFlow<Map<String, List<KidVideo>>>(emptyMap())
    val categoryVideosCache: StateFlow<Map<String, List<KidVideo>>> = _categoryVideosCache.asStateFlow()

    private val _kidsYoutubeSearchResults = MutableStateFlow<List<KidVideo>>(emptyList())

    private data class ParentFilter(
        val blockedCh: List<BlockedChannel>,
        val allowedCh: List<AllowedChannel>,
        val blockedW: List<BlockedWord>,
        val config: ParentConfig,
        val customApproved: List<KidVideo>
    )

    private val parentFilterState: Flow<ParentFilter> = combine(
        blockedChannels,
        allowedChannels,
        blockedWords,
        parentConfig,
        customApprovedVideos
    ) { blockedCh, allowedCh, blockedW, config, customApproved ->
        ParentFilter(blockedCh, allowedCh, blockedW, config, customApproved)
    }

    private val _recommendedVideos = MutableStateFlow<List<KidVideo>>(emptyList())
    val recommendedVideos: StateFlow<List<KidVideo>> = _recommendedVideos.asStateFlow()

    // --- Screen Timer State ---
    private val _screenTimeSpentSeconds = MutableStateFlow(0)
    val screenTimeSpentSeconds: StateFlow<Int> = _screenTimeSpentSeconds.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Run database automatic initial population
        viewModelScope.launch(Dispatchers.IO) {
            repository.autoPopulateDefaults()
            // Reset query search and categories
            onCategoryChange("Todas")
        }

        // Listen to category changes and fetch YouTube videos accordingly
        viewModelScope.launch {
            _selectedCategory.collectLatest { category ->
                fetchCategoryVideosIfNeeded(category)
            }
        }
        
        // Dynamic re-filtering of searchResults when blacklist/config/state updates in real-time
        viewModelScope.launch {
            combine(
                blockedWords,
                blockedChannels,
                allowedChannels,
                parentConfig
            ) { _, _, _, _ ->
                Unit
            }.debounce(150).collectLatest {
                filterVideos()
            }
        }

        // Dynamic fetching of real YouTube related videos when a video plays
        viewModelScope.launch {
            _activeVideo.collect { active ->
                if (active != null) {
                    val query = active.title
                    val results = GeminiService.searchYoutubePublicly(query)
                    val filtered = results.filter { video ->
                        if (video.id == active.id) return@filter false
                        val filter = parentFilterState.firstOrNull() ?: return@filter true
                        val blockedChList = filter.blockedCh.map { it.channelName.trim().lowercase() }
                        val videoChannel = video.channelName.trim().lowercase()
                        if (blockedChList.any { blocked -> videoChannel == blocked || videoChannel.contains(blocked) || blocked.contains(videoChannel) }) return@filter false
                        if (filter.config.isStrictChannelMode) {
                            val allowedChList = filter.allowedCh.map { it.channelName.trim().lowercase() }
                            if (allowedChList.none { allowed -> videoChannel == allowed || videoChannel.contains(allowed) }) return@filter false
                        }
                        val blockedWList = filter.blockedW.map { it.word.trim().lowercase() }
                        val titleText = video.title.lowercase()
                        for (word in blockedWList) {
                            if (titleText.contains(word)) return@filter false
                        }
                        true
                    }
                    _recommendedVideos.value = filtered
                } else {
                    _recommendedVideos.value = emptyList()
                }
            }
        }

        // Start checking total timer
        startGlobalTimer()
    }

    // --- Navigation actions ---
    fun navigateTo(screen: LeaoScreen) {
        if (screen == LeaoScreen.ProfileSelection) {
            _currentProfile.value = null
            _activeVideo.value = null
        }
        _currentScreen.value = screen
    }

    fun onStartClicked() {
        val email = parentConfig.value.connectedEmail
        if (email.isNullOrEmpty()) {
            navigateTo(LeaoScreen.Login)
        } else {
            navigateTo(LeaoScreen.ProfileSelection)
        }
    }

    fun selectProfileAndNavigate(profile: ChildProfile) {
        _currentProfile.value = profile
        _screenTimeSpentSeconds.value = 0 // reset screen time spent session
        loadProfileRelatedData(profile.id)
        navigateTo(LeaoScreen.ChildHome)
    }

    fun selectVideoAndNavigate(video: KidVideo) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Check if video is safe first (against blocked word list or block channel list or allowed channels)
            val isSafe = isVideoAllowed(video)
            if (!isSafe) {
                // If not safe, block play
                _unsafeSearchResponse.value = ModerationResult(
                    safe = false,
                    reason = "Este vídeo do canal '${video.channelName}' foi bloqueado devido às configurações de filtro parental.",
                    suggestedAlternative = "Escolha outro vídeo fofinho!"
                )
                navigateTo(LeaoScreen.ChildHome)
                return@launch
            }

            _activeVideo.value = video
            repository.registerWatchHistory(profile.id, video, 0)
            loadProfileRelatedData(profile.id)
            withContext(Dispatchers.Main) {
                navigateTo(LeaoScreen.Player)
            }
        }
    }

    private fun loadProfileRelatedData(profileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getFavorites(profileId).collect { favoritesList.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPlaylists(profileId).collect { playlistsList.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.getHistory(profileId).collect { historyList.value = it }
        }
    }

    // --- Parental Gate Challenge ---
    fun prepareParentGate(next: LeaoScreen) {
        firstGateNum.value = (2..9).random()
        secondGateNum.value = (2..9).random()
        gateAnswerInput.value = ""
        gateErrorMsg.value = ""
        _gateNextScreen.value = next
        navigateTo(LeaoScreen.ParentGate)
    }

    fun verifyParentGate() {
        val expected = firstGateNum.value * secondGateNum.value
        val userVal = gateAnswerInput.value.trim().toIntOrNull()
        if (userVal == expected) {
            navigateTo(_gateNextScreen.value)
        } else {
            gateErrorMsg.value = "Resposta errada! As crianças não passam daqui. Tente outra vez!"
            firstGateNum.value = (2..9).random()
            secondGateNum.value = (2..9).random()
            gateAnswerInput.value = ""
        }
    }

    // --- Custom video filters logic ---
    private suspend fun isVideoAllowed(video: KidVideo): Boolean {
        val config = parentConfig.value

        // 1. Channel blacklist verify
        val blockedChList = blockedChannels.value.map { it.channelName.trim().lowercase() }.filter { it.isNotEmpty() }
        val videoChannel = video.channelName.trim().lowercase()
        if (blockedChList.any { blocked -> videoChannel == blocked || videoChannel.contains(blocked) || blocked.contains(videoChannel) }) {
            return false
        }

        // 2. Strict Approved-only Mode verify
        if (config.isStrictChannelMode) {
            val allowedChList = allowedChannels.value.map { it.channelName.trim().lowercase() }.filter { it.isNotEmpty() }
            if (allowedChList.none { allowed -> videoChannel == allowed || videoChannel.contains(allowed) }) {
                return false
            }
        }

        // 3. Blacklist of words verify
        val blockedWList = blockedWords.value.map { it.word.trim().lowercase() }.filter { it.isNotEmpty() }
        val titleText = video.title.lowercase()
        val descText = video.description.lowercase()
        for (word in blockedWList) {
            if (titleText.contains(word) || descText.contains(word)) {
                return false
            }
        }

        return true
    }

    // --- Search & Filter methods ---
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _kidsYoutubeSearchResults.value = emptyList()
        filterVideos()
    }

    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
        filterVideos()
    }

    fun fetchCategoryVideosIfNeeded(category: String) {
        val currentCache = _categoryVideosCache.value
        if (currentCache[category].orEmpty().isNotEmpty()) {
            filterVideos()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val query = when (category) {
                "Todas" -> "desenho educativo infantil completo"
                "Astronomia" -> "sistema solar para crianças"
                "Dinossauros" -> "dinossauros para crianças"
                "Ciências" -> "manual do mundo experiencias simples"
                "Música" -> "musica infantil bita"
                "Artes" -> "desenho infantil facil"
                else -> "desenho educativo infantil"
            }

            withContext(Dispatchers.Main) {
                _isCategoryLoading.value = true
            }

            try {
                val results = GeminiService.searchYoutubePublicly(query)
                val filtered = results.filter { isVideoAllowed(it) }.map { it.copy(category = category) }
                _categoryVideosCache.value = _categoryVideosCache.value + (category to filtered)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    _isCategoryLoading.value = false
                }
                filterVideos()
            }
        }
    }

    fun performKidsSearch(query: String) {
        if (query.trim().isEmpty()) {
            _kidsYoutubeSearchResults.value = emptyList()
            filterVideos()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Check local moderation first
            val localModeration = GeminiService.evaluateLocally(query)
            if (!localModeration.safe) {
                val profile = _currentProfile.value
                if (profile != null) {
                    repository.logSearchBlock(profile.id, query)
                }
                _unsafeSearchResponse.value = localModeration
                _searchResults.value = emptyList()
                return@launch
            }

            val config = parentConfig.value
            if (config.isSmartCuratorMode) {
                withContext(Dispatchers.Main) {
                    _isSmartCurating.value = true
                }
                val aiModeration = GeminiService.auditContent(query)
                withContext(Dispatchers.Main) {
                    _isSmartCurating.value = false
                }

                if (!aiModeration.safe) {
                    val profile = _currentProfile.value
                    if (profile != null) {
                        repository.logSearchBlock(profile.id, query)
                    }
                    _unsafeSearchResponse.value = aiModeration
                    _searchResults.value = emptyList()
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                _isSmartCurating.value = true
            }
            try {
                val results = GeminiService.searchYoutubePublicly(query)
                val filtered = results.filter { isVideoAllowed(it) }
                _kidsYoutubeSearchResults.value = filtered
                _unsafeSearchResponse.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    _isSmartCurating.value = false
                }
                filterVideos()
            }
        }
    }

    private fun filterVideos() {
        val query = _searchQuery.value.trim()
        val category = _selectedCategory.value

        viewModelScope.launch(Dispatchers.IO) {
            val approvedVideos = customApprovedVideos.value
            val categoryVideos = _categoryVideosCache.value[category].orEmpty()
            val kidsSearchResults = _kidsYoutubeSearchResults.value

            // 1. Determine the source list
            val sourceVideos = if (query.isNotEmpty() && kidsSearchResults.isNotEmpty()) {
                (approvedVideos + kidsSearchResults).distinctBy { it.id }
            } else if (query.isNotEmpty()) {
                (approvedVideos + categoryVideos + repository.onlineYoutubeVideos).distinctBy { it.id }
            } else {
                (approvedVideos + categoryVideos).distinctBy { it.id }
            }

            // 2. Filter by category, query and parental rules
            val filtered = sourceVideos.filter { video ->
                val isNotBlocked = isVideoAllowed(video)
                if (!isNotBlocked) return@filter false

                if (query.isNotEmpty() && kidsSearchResults.isNotEmpty()) {
                    // Show search results directly (they are already generated for the query)
                    true
                } else {
                    val matchesCategory = (category == "Todas" || video.category.lowercase() == category.lowercase())
                    val matchesQuery = (query.isEmpty() || video.title.lowercase().contains(query.lowercase()) ||
                            video.channelName.lowercase().contains(query.lowercase()))
                    matchesCategory && matchesQuery
                }
            }

            _unsafeSearchResponse.value = null
            _searchResults.value = filtered
        }
    }

    fun clearUnsafeSearchBlock() {
        _unsafeSearchResponse.value = null
        _searchQuery.value = ""
        filterVideos()
    }

    fun searchYoutubeAsParent(query: String) {
        _parentYoutubeSearchQuery.value = query
        if (query.trim().isEmpty()) {
            _parentYoutubeSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var realResults = emptyList<KidVideo>()
            val apiKey = GeminiService.getGeminiApiKey()
            if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                realResults = GeminiService.searchYoutubeViaAI(query)
            }
            if (realResults.isEmpty()) {
                realResults = GeminiService.searchYoutubePublicly(query)
            }
            _parentYoutubeSearchResults.value = realResults
        }
    }

    fun approveVideoForApp(video: KidVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveApprovedVideo(video)
            filterVideos()
        }
    }

    fun removeApprovedVideo(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteApprovedVideo(videoId)
            filterVideos()
        }
    }

    fun importSampleVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val samples = listOf(
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
                    category = "Desenhos",
                    description = "Peppa, George e seus amiguinhos vão ao planetário aprender sobre as estrelas e a lua."
                )
            )
            samples.forEach { repository.saveApprovedVideo(it) }
            filterVideos()
        }
    }

    // --- Favorites Actions ---
    fun toggleFavorite(video: KidVideo) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(profile.id, video)
            loadProfileRelatedData(profile.id)
        }
    }

    fun isFavorite(videoId: String): Boolean {
        return favoritesList.value.any { it.videoId == videoId }
    }

    // --- Dynamic screen timer tracking ---
    private fun startGlobalTimer() {
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                // Increment spent seconds only if player screen is active
                if (_currentScreen.value == LeaoScreen.Player) {
                    _screenTimeSpentSeconds.value += 1
                    val limitSecs = parentConfig.value.screenTimeLimitMinutes * 60
                    if (_screenTimeSpentSeconds.value >= limitSecs) {
                        // Limit matched! Switch to Warning Screen
                        _currentScreen.value = LeaoScreen.LimitsExceeded
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // --- Database CRUD wrappers for parent settings panel ---

    fun addBlockedWord(word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBlockedWord(word)
        }
    }

    fun removeBlockedWord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBlockedWord(id)
        }
    }

    fun addBlockedChannel(channel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBlockedChannel(channel)
        }
    }

    fun removeBlockedChannel(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBlockedChannel(id)
        }
    }

    fun addAllowedChannel(channel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addAllowedChannel(channel)
        }
    }

    fun removeAllowedChannel(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeAllowedChannel(id)
        }
    }

    fun updateScreenTimeTimer(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = parentConfig.value
            repository.saveConfig(curr.copy(screenTimeLimitMinutes = minutes))
        }
    }

    fun changeStrictChannelMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = parentConfig.value
            repository.saveConfig(curr.copy(isStrictChannelMode = enabled))
        }
    }

    fun changeSmartCuratorMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = parentConfig.value
            repository.saveConfig(curr.copy(isSmartCuratorMode = enabled))
        }
    }

    fun connectMockGoogleAccount(email: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = parentConfig.value
            repository.saveConfig(
                curr.copy(
                    connectedEmail = email,
                    connectedName = name,
                    connectedPhoto = "https://lh3.googleusercontent.com/aida-public/AB6AXuCQfH04TaTbwky6VSzp-QjQQNjH1OnYtW3dHgW_XaJ9tccO535ioVVJZsc1R2I2-pyAyzT9gPBio_Coc5pB3qVLyECDjpcYcoa2GlxR4ts8_VvLrXiUqc1AvAYFr--KKIpyFpLi1QNajJeqPlUTZ1LsvY9ObdBbZ89-t8yA-Tm4Gcvkc9-95amOAimXO6fMQ0G4iyV0QKVV7xjfmR4_aTQ6CuH7A-bJSclVgYfARR7UxcZLP0L8cg8wii2x4k2T1Le2GAxA34Jcd2w"
                )
            )
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = parentConfig.value
            repository.saveConfig(
                curr.copy(
                    connectedEmail = null,
                    connectedName = null,
                    connectedPhoto = null
                )
            )
        }
    }

    fun clearBlockedSearchLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearBlockedLogs()
        }
    }

    // --- Custom Playlists ---

    fun createNewPlaylist(name: String) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.createPlaylist(profile.id, name)
            loadProfileRelatedData(profile.id)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlistId)
            loadProfileRelatedData(profile.id)
        }
    }

    fun addVideoToPlaylist(playlistId: Long, video: KidVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addVideoToPlaylist(playlistId, video)
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    // --- New Child Profiles Creation ---
    val availableAvatars = listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCdXv6aunqcY7UKJnaAGtqyXESXavtwtHnAf9EmU1fqyiHEatwzRGybdOE1L1NGKzckVLeqqYKG3elQDIDitkkJYKjnxFhKqI0HKHRbmH2Ccmr_sDUxgk1yQ3kW36S23gj_zhBM8A2chuYSWGFBokQ7AwO93LcMQAasXPJds_srS1MCOLorwaLFOkWyCsZGxN-pAUFjoZ9XMZLfYKGMknOVs22rAwJ0pFdLFcfGfxzLlSNIe57FjYpQw2Ht80sbPe6mXVq0YNjh-Z8" to "Astronauta",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuC7ugoftgcrSMmMHtclryDr_ufH5WJjO05gxQ6HjaSwEVBbek91fqUH6i0y1n6YGSqoc1CmM7O2kB4d1ogx9vljivVCv1I9MBYj7OchX3B9LnZVIDBHwnKoimbdJcPHEqf8CfZWpiXi_JlhpmLPsWHZTv8ORU4Ym73ZI3dtZC95O-BKdRDSvuQwT2teQRkt5qNyrBZc-NYxdq-Muos4Z9pS7p6lsV7hcq0Uw5OYRjGm1zLn2pRD6JRVTt0I-AqhwoDLQDHhcT3YvL8" to "Fadinha",
        "https://api.dicebear.com/7.x/bottts/png?seed=Noah&backgroundColor=b6e3f4" to "Robozinho de Estrelas",
        "https://api.dicebear.com/7.x/adventurer/png?seed=Eloah&backgroundColor=ffd5dc" to "Aventureira Eloáh",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=cat&backgroundColor=c0afea" to "Gatinho Dinossauro",
        "https://api.dicebear.com/7.x/adventurer/png?seed=magic&backgroundColor=f4bbf9" to "Estrela de Cristal"
    )

    fun createChildProfile(name: String, isBoy: Boolean, avatarUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedAvatar = avatarUrl ?: if (isBoy) {
                availableAvatars[0].first
            } else {
                availableAvatars[1].first
            }
            repository.createProfile(name, isBoy, selectedAvatar)
        }
    }

    fun updateChildProfile(id: Long, name: String, isBoy: Boolean, avatarUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = ChildProfile(
                id = id,
                name = name,
                isBoy = isBoy,
                avatarUrl = avatarUrl
            )
            repository.updateProfile(updated)
            // If it's the currently selected child, refresh the active profile state
            if (_currentProfile.value?.id == id) {
                _currentProfile.value = updated
            }
        }
    }

    fun deleteChildProfile(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeProfile(id)
            if (_currentProfile.value?.id == id) {
                _currentProfile.value = null
            }
        }
    }

    fun resetApp() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            repository.autoPopulateDefaults()
            withContext(Dispatchers.Main) {
                navigateTo(LeaoScreen.Splash)
            }
        }
    }

    // --- Factory ---
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LeaoViewModel(application) as T
            }
        }
    }
}
