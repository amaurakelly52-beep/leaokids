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

    val recommendedVideos: StateFlow<List<KidVideo>> = combine(
        _activeVideo,
        blockedChannels,
        allowedChannels,
        blockedWords,
        parentConfig
    ) { active, blockedCh, allowedCh, blockedW, config ->
        val currentActive = active ?: return@combine emptyList()
        presetVideos.filter { video ->
            if (video.id == currentActive.id) return@filter false

            // 1. Channel blacklist verify
            val blockedChList = blockedCh.map { it.channelName.lowercase() }
            if (blockedChList.contains(video.channelName.lowercase())) return@filter false

            // 2. Strict Approved-only Mode verify
            if (config.isStrictChannelMode) {
                val allowedChList = allowedCh.map { it.channelName.lowercase() }
                if (!allowedChList.contains(video.channelName.lowercase())) return@filter false
            }

            // 3. Blacklist of words verify
            val blockedWList = blockedW.map { it.word.lowercase() }
            val titleText = video.title.lowercase()
            val descText = video.description.lowercase()
            for (word in blockedWList) {
                if (titleText.contains(word) || descText.contains(word)) return@filter false
            }

            true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        val blockedChList = blockedChannels.value.map { it.channelName.lowercase() }
        if (blockedChList.contains(video.channelName.lowercase())) {
            return false
        }

        // 2. Strict Approved-only Mode verify
        if (config.isStrictChannelMode) {
            val allowedChList = allowedChannels.value.map { it.channelName.lowercase() }
            if (!allowedChList.contains(video.channelName.lowercase())) {
                return false
            }
        }

        // 3. Blacklist of words verify
        val blockedWList = blockedWords.value.map { it.word.lowercase() }
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
        filterVideos()
    }

    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
        filterVideos()
    }

    private fun filterVideos() {
        val query = _searchQuery.value.trim()
        val category = _selectedCategory.value

        viewModelScope.launch(Dispatchers.IO) {
            if (query.isNotEmpty()) {
                val config = parentConfig.value
                val profile = _currentProfile.value

                // Simple check against local blocked words first
                val localModeration = GeminiService.evaluateLocally(query)
                if (!localModeration.safe) {
                    if (profile != null) {
                        repository.logSearchBlock(profile.id, query)
                    }
                    _unsafeSearchResponse.value = localModeration
                    _searchResults.value = emptyList()
                    return@launch
                }

                // If parent enabled AI curating, check Gemini API
                if (config.isSmartCuratorMode) {
                    _isSmartCurating.value = true
                    val aiModeration = GeminiService.auditContent(query)
                    _isSmartCurating.value = false

                    if (!aiModeration.safe) {
                        if (profile != null) {
                            repository.logSearchBlock(profile.id, query)
                        }
                        _unsafeSearchResponse.value = aiModeration
                        _searchResults.value = emptyList()
                        return@launch
                    }
                }
            }

            // Word and channel filters
            val filtered = repository.presetVideos.filter { video ->
                val matchesCategory = (category == "Todas" || video.category.lowercase() == category.lowercase())
                val matchesQuery = (query.isEmpty() || video.title.lowercase().contains(query.lowercase()) ||
                        video.channelName.lowercase().contains(query.lowercase()))
                val isNotBlocked = isVideoAllowed(video)

                matchesCategory && matchesQuery && isNotBlocked
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
    fun createChildProfile(name: String, isBoy: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val avatar = if (isBoy) {
                // Smile Boy URL avatar
                "https://lh3.googleusercontent.com/aida-public/AB6AXuCdXv6aunqcY7UKJnaAGtqyXESXavtwtHnAf9EmU1fqyiHEatwzRGybdOE1L1NGKzckVLeqqYKG3elQDIDitkkJYKjnxFhKqI0HKHRbmH2Ccmr_sDUxgk1yQ3kW36S23gj_zhBM8A2chuYSWGFBokQ7AwO93LcMQAasXPJds_srS1MCOLorwaLFOkWyCsZGxN-pAUFjoZ9XMZLfYKGMknOVs22rAwJ0pFdLFcfGfxzLlSNIe57FjYpQw2Ht80sbPe6mXVq0YNjh-Z8"
            } else {
                // Smile Girl URL avatar
                "https://lh3.googleusercontent.com/aida-public/AB6AXuC7ugoftgcrSMmMHtclryDr_ufH5WJjO05gxQ6HjaSwEVBbek91fqUH6i0y1n6YGSqoc1CmM7O2kB4d1ogx9vljivVCv1I9MBYj7OchX3B9LnZVIDBHwnKoimbdJcPHEqf8CfZWpiXi_JlhpmLPsWHZTv8ORU4Ym73ZI3dtZC95O-BKdRDSvuQwT2teQRkt5qNyrBZc-NYxdq-Muos4Z9pS7p6lsV7hcq0Uw5OYRjGm1zLn2pRD6JRVTt0I-AqhwoDLQDHhcT3YvL8"
            }
            repository.createProfile(name, isBoy, avatar)
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
