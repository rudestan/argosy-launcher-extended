package com.nendo.argosy.ui.quickmenu

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nendo.argosy.data.local.dao.SearchCandidate
import com.nendo.argosy.data.repository.AppsRepository
import com.nendo.argosy.data.repository.GameRepository
import com.nendo.argosy.data.repository.PlatformRepository
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.domain.usecase.quickmenu.GetTopUnplayedUseCase
import com.nendo.argosy.ui.common.displayTitleId
import com.nendo.argosy.ui.screens.common.LibrarySyncBus
import com.nendo.argosy.util.FuzzySearch
import com.nendo.argosy.util.formatPlayTime
import com.nendo.argosy.util.formatRelativeTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuickMenuOrb {
    SEARCH, RANDOM, MOST_PLAYED, TOP_UNPLAYED, RECENT, FAVORITES, APPS
}

enum class MetadataType {
    NONE, RATING, PLAY_TIME, RELATIVE_TIME
}

data class GameRowUi(
    val id: Long,
    val title: String,
    val platformName: String?,
    val coverPath: String?,
    val metadata: String,
    val metadataType: MetadataType = MetadataType.NONE,
    val isDownloaded: Boolean,
    val titleId: String? = null
)

data class GameCardUi(
    val id: Long,
    val title: String,
    val platformName: String?,
    val coverPath: String?,
    val year: Int?,
    val developer: String?,
    val rating: Float?,
    val genre: String?,
    val isDownloaded: Boolean
)

data class QuickMenuAppUi(
    val packageName: String,
    val label: String
)

data class QuickMenuUiState(
    val isVisible: Boolean = false,
    val selectedOrb: QuickMenuOrb = QuickMenuOrb.MOST_PLAYED,
    val contentFocused: Boolean = false,
    val focusedContentIndex: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<GameRowUi> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val searchInputFocused: Boolean = true,
    val randomGame: GameCardUi? = null,
    val mostPlayedGames: List<GameRowUi> = emptyList(),
    val topUnplayedGames: List<GameRowUi> = emptyList(),
    val recentGames: List<GameRowUi> = emptyList(),
    val favoriteGames: List<GameRowUi> = emptyList(),
    val quickMenuApps: List<QuickMenuAppUi> = emptyList(),
    val pickerInstalledApps: List<QuickMenuAppUi> = emptyList(),
    val pickerSystemApps: List<QuickMenuAppUi> = emptyList(),
    val pickerHiddenApps: List<QuickMenuAppUi> = emptyList(),
    val showAppPicker: Boolean = false,
    val appPickerFocusIndex: Int = 0,
    val isLoading: Boolean = false
) {
    val pickerAllApps: List<QuickMenuAppUi>
        get() = pickerInstalledApps + pickerSystemApps + pickerHiddenApps
}

private const val LIST_LIMIT = 20
internal const val QUICK_MENU_APP_GRID_COLUMNS = 4

@HiltViewModel
class QuickMenuViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameRepository: GameRepository,
    private val platformRepository: PlatformRepository,
    private val appsRepository: AppsRepository,
    private val getTopUnplayedUseCase: GetTopUnplayedUseCase,
    private val librarySyncBus: LibrarySyncBus,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickMenuUiState())
    val uiState: StateFlow<QuickMenuUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val platformCache = mutableMapOf<Long, String>()
    private var searchCandidates: List<SearchCandidate>? = null

    init {
        viewModelScope.launch {
            librarySyncBus.syncCompleted.collect {
                invalidateSearchCache()
            }
        }
    }

    fun show() {
        _uiState.update {
            QuickMenuUiState(
                isVisible = true,
                selectedOrb = QuickMenuOrb.MOST_PLAYED,
                contentFocused = false,
                focusedContentIndex = 0
            )
        }
        loadAllData()
    }

    fun hide() {
        _uiState.update { it.copy(isVisible = false) }
    }

    fun toggle() {
        if (_uiState.value.isVisible) hide() else show()
    }

    fun selectOrb(orb: QuickMenuOrb) {
        _uiState.update {
            it.copy(
                selectedOrb = orb,
                focusedContentIndex = 0
            )
        }
    }

    fun moveOrbLeft() {
        val orbs = QuickMenuOrb.entries
        val currentIndex = orbs.indexOf(_uiState.value.selectedOrb)
        val newIndex = if (currentIndex > 0) currentIndex - 1 else orbs.lastIndex
        selectOrb(orbs[newIndex])
    }

    fun moveOrbRight() {
        val orbs = QuickMenuOrb.entries
        val currentIndex = orbs.indexOf(_uiState.value.selectedOrb)
        val newIndex = if (currentIndex < orbs.lastIndex) currentIndex + 1 else 0
        selectOrb(orbs[newIndex])
    }

    fun enterContent() {
        val isSearch = _uiState.value.selectedOrb == QuickMenuOrb.SEARCH
        _uiState.update {
            it.copy(
                contentFocused = true,
                focusedContentIndex = 0,
                searchInputFocused = isSearch
            )
        }
    }

    fun exitContent() {
        _uiState.update { it.copy(contentFocused = false, searchInputFocused = true) }
    }

    fun moveContentUp() {
        _uiState.update { state ->
            when {
                state.selectedOrb == QuickMenuOrb.APPS ->
                    state.copy(focusedContentIndex = appFocusIndex(state, dx = 0, dy = -1))
                state.selectedOrb == QuickMenuOrb.SEARCH && !state.searchInputFocused && state.focusedContentIndex == 0 ->
                    state.copy(searchInputFocused = true)
                else ->
                    state.copy(focusedContentIndex = (state.focusedContentIndex - 1).coerceAtLeast(0))
            }
        }
    }

    fun moveContentDown() {
        _uiState.update { state ->
            when {
                state.selectedOrb == QuickMenuOrb.APPS ->
                    state.copy(focusedContentIndex = appFocusIndex(state, dx = 0, dy = 1))
                state.selectedOrb == QuickMenuOrb.SEARCH && state.searchInputFocused -> {
                    val hasItems = if (state.searchQuery.length < 2) {
                        state.recentSearches.isNotEmpty()
                    } else {
                        state.searchResults.isNotEmpty()
                    }
                    if (hasItems) {
                        state.copy(searchInputFocused = false, focusedContentIndex = 0)
                    } else {
                        state
                    }
                }
                else -> {
                    val maxIndex = getCurrentContentSize() - 1
                    val newIndex = (state.focusedContentIndex + 1).coerceAtMost(maxIndex.coerceAtLeast(0))
                    state.copy(focusedContentIndex = newIndex)
                }
            }
        }
    }

    fun moveContentLeft() {
        _uiState.update { state ->
            if (state.selectedOrb == QuickMenuOrb.APPS) {
                state.copy(focusedContentIndex = appFocusIndex(state, dx = -1, dy = 0))
            } else {
                state
            }
        }
    }

    fun moveContentRight() {
        _uiState.update { state ->
            if (state.selectedOrb == QuickMenuOrb.APPS) {
                state.copy(focusedContentIndex = appFocusIndex(state, dx = 1, dy = 0))
            } else {
                state
            }
        }
    }

    private fun appFocusIndex(state: QuickMenuUiState, dx: Int, dy: Int): Int {
        val size = state.quickMenuApps.size + 1
        val current = state.focusedContentIndex
        return when {
            dy != 0 -> {
                val target = current + dy * QUICK_MENU_APP_GRID_COLUMNS
                if (target in 0 until size) target else current
            }
            dx < 0 -> if (current % QUICK_MENU_APP_GRID_COLUMNS > 0) current - 1 else current
            dx > 0 ->
                if (current % QUICK_MENU_APP_GRID_COLUMNS < QUICK_MENU_APP_GRID_COLUMNS - 1 && current + 1 < size) current + 1 else current
            else -> current
        }
    }

    fun getSelectedGameId(): Long? {
        val state = _uiState.value
        if (!state.contentFocused) return null

        return when (state.selectedOrb) {
            QuickMenuOrb.SEARCH -> {
                if (state.searchQuery.length < 2) null
                else state.searchResults.getOrNull(state.focusedContentIndex)?.id
            }
            QuickMenuOrb.RANDOM -> state.randomGame?.id
            QuickMenuOrb.MOST_PLAYED -> state.mostPlayedGames.getOrNull(state.focusedContentIndex)?.id
            QuickMenuOrb.TOP_UNPLAYED -> state.topUnplayedGames.getOrNull(state.focusedContentIndex)?.id
            QuickMenuOrb.RECENT -> state.recentGames.getOrNull(state.focusedContentIndex)?.id
            QuickMenuOrb.FAVORITES -> state.favoriteGames.getOrNull(state.focusedContentIndex)?.id
            QuickMenuOrb.APPS -> null
        }
    }

    fun getSelectedAppPackage(): String? {
        val state = _uiState.value
        if (!state.contentFocused) return null
        if (state.selectedOrb != QuickMenuOrb.APPS) return null
        return state.quickMenuApps.getOrNull(state.focusedContentIndex)?.packageName
    }

    fun getLaunchIntent(packageName: String): Intent? =
        appsRepository.getLaunchIntent(packageName)

    fun isAddAppTileFocused(): Boolean {
        val state = _uiState.value
        return state.contentFocused &&
            state.selectedOrb == QuickMenuOrb.APPS &&
            state.focusedContentIndex == state.quickMenuApps.size
    }

    fun openAppPicker() {
        _uiState.update { it.copy(showAppPicker = true, appPickerFocusIndex = 0) }
    }

    fun closeAppPicker() {
        _uiState.update { it.copy(showAppPicker = false, appPickerFocusIndex = 0) }
    }

    fun moveAppPickerFocus(delta: Int) {
        _uiState.update { state ->
            val maxIndex = state.pickerAllApps.size - 1
            if (maxIndex < 0) return@update state
            state.copy(appPickerFocusIndex = (state.appPickerFocusIndex + delta).coerceIn(0, maxIndex))
        }
    }

    fun selectAppFromPicker() {
        val packageName = _uiState.value.pickerAllApps
            .getOrNull(_uiState.value.appPickerFocusIndex)
            ?.packageName ?: return
        closeAppPicker()
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            preferencesRepository.setQuickMenuApps(prefs.quickMenuApps + packageName)
            loadApps()
        }
    }

    fun isOnRecentSearches(): Boolean {
        val state = _uiState.value
        return state.selectedOrb == QuickMenuOrb.SEARCH &&
            state.contentFocused &&
            !state.searchInputFocused &&
            state.searchQuery.length < 2 &&
            state.recentSearches.isNotEmpty()
    }

    fun selectRecentSearch(index: Int) {
        val state = _uiState.value
        val query = state.recentSearches.getOrNull(index) ?: return
        selectRecentSearch(query)
    }

    fun selectRecentSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, focusedContentIndex = 0) }
        performSearch(query)
    }

    fun saveSearchQuery() {
        val query = _uiState.value.searchQuery
        if (query.length >= 2) {
            viewModelScope.launch {
                preferencesRepository.addLibraryRecentSearch(query)
                loadRecentSearches()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        performSearch(query)
    }

    fun rerollRandom() {
        viewModelScope.launch {
            loadRandomGame()
        }
    }

    private fun getCurrentContentSize(): Int {
        val state = _uiState.value
        return when (state.selectedOrb) {
            QuickMenuOrb.SEARCH -> {
                if (state.searchQuery.length < 2) state.recentSearches.size
                else state.searchResults.size
            }
            QuickMenuOrb.RANDOM -> if (state.randomGame != null) 1 else 0
            QuickMenuOrb.MOST_PLAYED -> state.mostPlayedGames.size
            QuickMenuOrb.TOP_UNPLAYED -> state.topUnplayedGames.size
            QuickMenuOrb.RECENT -> state.recentGames.size
            QuickMenuOrb.FAVORITES -> state.favoriteGames.size
            QuickMenuOrb.APPS -> state.quickMenuApps.size + 1
        }
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            launch { loadMostPlayed() }
            launch { loadTopUnplayed() }
            launch { loadRecent() }
            launch { loadFavorites() }
            launch { loadRandomGame() }
            launch { loadRecentSearches() }
            launch { loadApps() }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadApps() {
        val prefs = preferencesRepository.preferences.first()
        val pinned = prefs.quickMenuApps
        val hidden = prefs.hiddenApps
        val installed = appsRepository.getInstalledApps(includeSystemApps = true)
            .filter { !it.isArgosy }
        val pinnedApps = installed
            .filter { it.packageName in pinned }
            .map { QuickMenuAppUi(it.packageName, it.label) }
            .sortedBy { it.label.lowercase() }
        val pickerInstalledApps = installed
            .filter { it.packageName !in pinned && it.packageName !in hidden && !it.isSystemApp }
            .map { QuickMenuAppUi(it.packageName, it.label) }
            .sortedBy { it.label.lowercase() }
        val pickerSystemApps = installed
            .filter { it.packageName !in pinned && it.packageName !in hidden && it.isSystemApp }
            .map { QuickMenuAppUi(it.packageName, it.label) }
            .sortedBy { it.label.lowercase() }
        val pickerHiddenApps = installed
            .filter { it.packageName !in pinned && it.packageName in hidden }
            .map { QuickMenuAppUi(it.packageName, it.label) }
            .sortedBy { it.label.lowercase() }
        _uiState.update {
            it.copy(
                quickMenuApps = pinnedApps,
                pickerInstalledApps = pickerInstalledApps,
                pickerSystemApps = pickerSystemApps,
                pickerHiddenApps = pickerHiddenApps
            )
        }
    }

    private suspend fun loadRecentSearches() {
        val prefs = preferencesRepository.preferences.first()
        _uiState.update { it.copy(recentSearches = prefs.libraryRecentSearches) }
    }

    private suspend fun loadMostPlayed() {
        val games = gameRepository.getPlayedGames().take(LIST_LIMIT)
        val rows = games.map { it.toGameRowUi(MetadataType.PLAY_TIME) { formatPlayTime(context, it.playTimeMinutes) } }
        _uiState.update { it.copy(mostPlayedGames = rows) }
    }

    private suspend fun loadTopUnplayed() {
        val games = getTopUnplayedUseCase(LIST_LIMIT).first()
        val rows = games.map { it.toGameRowUi(MetadataType.RATING) { formatRating(it.rating) } }
        _uiState.update { it.copy(topUnplayedGames = rows) }
    }

    private suspend fun loadRecent() {
        val games = gameRepository.getRecentlyPlayed(LIST_LIMIT)
        val rows = games.map { it.toGameRowUi(MetadataType.RELATIVE_TIME) { formatRelativeTime(context, it.lastPlayed) } }
        _uiState.update { it.copy(recentGames = rows) }
    }

    private suspend fun loadFavorites() {
        val games = gameRepository.getFavorites().take(LIST_LIMIT)
        val rows = games.map { it.toGameRowUi(MetadataType.NONE) { "" } }
        _uiState.update { it.copy(favoriteGames = rows) }
    }

    private suspend fun loadRandomGame() {
        val game = gameRepository.getRandomGame()
        val card = game?.toGameCardUi()
        _uiState.update { it.copy(randomGame = card) }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()

        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            val candidates = searchCandidates ?: gameRepository.getSearchCandidates().also {
                searchCandidates = it
            }
            val matched = FuzzySearch.search(query, candidates, limit = 10)
            if (matched.isEmpty()) {
                _uiState.update { it.copy(searchResults = emptyList(), focusedContentIndex = 0) }
                return@launch
            }
            val matchedIds = matched.map { it.id }
            val games = gameRepository.getByIds(matchedIds)
            val gamesById = games.associateBy { it.id }
            val orderedGames = matchedIds.mapNotNull { gamesById[it] }
            val rows = orderedGames.map { it.toGameRowUi(MetadataType.RATING) { formatRating(it.rating) } }
            _uiState.update { it.copy(searchResults = rows, focusedContentIndex = 0) }
        }
    }

    fun invalidateSearchCache() {
        searchCandidates = null
    }

    private suspend fun GameEntity.toGameRowUi(
        metadataType: MetadataType,
        metadataProvider: () -> String
    ): GameRowUi {
        val platformName = getPlatformName(platformId)
        return GameRowUi(
            id = id,
            title = title,
            platformName = platformName,
            coverPath = coverPath,
            metadata = metadataProvider(),
            metadataType = metadataType,
            isDownloaded = localPath != null,
            titleId = displayTitleId
        )
    }

    private suspend fun GameEntity.toGameCardUi(): GameCardUi {
        val platformName = getPlatformName(platformId)
        return GameCardUi(
            id = id,
            title = title,
            platformName = platformName,
            coverPath = coverPath,
            year = releaseYear,
            developer = developer,
            rating = rating,
            genre = genre,
            isDownloaded = localPath != null
        )
    }

    private suspend fun getPlatformName(platformId: Long): String? {
        platformCache[platformId]?.let { return it }
        val name = platformRepository.getById(platformId)?.name ?: return null
        platformCache[platformId] = name
        return name
    }

    private fun formatRating(rating: Float?): String {
        return rating?.let { "${it.toInt()}%" } ?: ""
    }
}
