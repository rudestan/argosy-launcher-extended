package com.nendo.argosy.ui.quickmenu.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.nendo.argosy.ui.util.clickableNoFocus
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.nendo.argosy.R
import com.nendo.argosy.ui.coil.AppIconData
import com.nendo.argosy.ui.common.rememberFileImageModel
import com.nendo.argosy.ui.components.animateScrollToItemCentered
import com.nendo.argosy.ui.quickmenu.GameCardUi
import com.nendo.argosy.ui.quickmenu.GameRowUi
import com.nendo.argosy.ui.quickmenu.QUICK_MENU_APP_GRID_COLUMNS
import com.nendo.argosy.ui.quickmenu.QuickMenuAppUi
import com.nendo.argosy.ui.quickmenu.QuickMenuOrb
import com.nendo.argosy.ui.quickmenu.QuickMenuUiState
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.LocalLauncherTheme

@Composable
fun QuickMenuContent(
    uiState: QuickMenuUiState,
    isFocused: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onGameSelect: (Long) -> Unit,
    onRecentSearchSelect: (String) -> Unit,
    onAppLaunch: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onAddApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (isFocused) 1f else 0.7f

    AnimatedContent(
        targetState = uiState.selectedOrb,
        transitionSpec = {
            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(100))
        },
        modifier = modifier.alpha(contentAlpha),
        label = "quickMenuContent"
    ) { orb ->
        when (orb) {
            QuickMenuOrb.SEARCH -> SearchContent(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                recentSearches = uiState.recentSearches,
                focusedIndex = uiState.focusedContentIndex,
                isInputFocused = isFocused && uiState.searchInputFocused,
                isListFocused = isFocused && !uiState.searchInputFocused,
                onQueryChange = onSearchQueryChange,
                onGameSelect = onGameSelect,
                onRecentSearchSelect = onRecentSearchSelect
            )
            QuickMenuOrb.RANDOM -> RandomContent(
                game = uiState.randomGame,
                isFocused = isFocused,
                onClick = { uiState.randomGame?.id?.let { onGameSelect(it) } }
            )
            QuickMenuOrb.MOST_PLAYED -> ListContent(
                games = uiState.mostPlayedGames,
                focusedIndex = uiState.focusedContentIndex,
                isFocused = isFocused,
                emptyMessage = stringResource(R.string.ui_quick_menu_empty_most_played),
                onGameSelect = onGameSelect
            )
            QuickMenuOrb.TOP_UNPLAYED -> ListContent(
                games = uiState.topUnplayedGames,
                focusedIndex = uiState.focusedContentIndex,
                isFocused = isFocused,
                emptyMessage = stringResource(R.string.ui_quick_menu_empty_top_unplayed),
                onGameSelect = onGameSelect
            )
            QuickMenuOrb.RECENT -> ListContent(
                games = uiState.recentGames,
                focusedIndex = uiState.focusedContentIndex,
                isFocused = isFocused,
                emptyMessage = stringResource(R.string.ui_quick_menu_empty_recent),
                onGameSelect = onGameSelect
            )
            QuickMenuOrb.FAVORITES -> ListContent(
                games = uiState.favoriteGames,
                focusedIndex = uiState.focusedContentIndex,
                isFocused = isFocused,
                emptyMessage = stringResource(R.string.ui_quick_menu_empty_favorites),
                onGameSelect = onGameSelect
            )
            QuickMenuOrb.APPS -> AppsContent(
                apps = uiState.quickMenuApps,
                focusedIndex = uiState.focusedContentIndex,
                isFocused = isFocused,
                onAppLaunch = onAppLaunch,
                onRemoveApp = onRemoveApp,
                onAddApp = onAddApp,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SearchContent(
    query: String,
    results: List<GameRowUi>,
    recentSearches: List<String>,
    focusedIndex: Int,
    isInputFocused: Boolean,
    isListFocused: Boolean,
    onQueryChange: (String) -> Unit,
    onGameSelect: (Long) -> Unit,
    onRecentSearchSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val inputShape = RoundedCornerShape(Dimens.radiusLg)
    val inputBorderModifier = if (isInputFocused) {
        Modifier.border(Dimens.borderMedium, MaterialTheme.colorScheme.primary, inputShape)
    } else Modifier

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(inputBorderModifier)
                .background(
                    if (isInputFocused) LocalArgosyTheme.current.focusAccent.copy(alpha = 0.15f)
                        .compositeOver(MaterialTheme.colorScheme.surface)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    inputShape
                )
                .padding(horizontal = Dimens.spacingMd, vertical = Dimens.radiusLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconSm + Dimens.borderMedium)
            )
            Spacer(modifier = Modifier.width(Dimens.radiusLg))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ui_quick_menu_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacingMd))

        if (query.length < 2) {
            if (recentSearches.isNotEmpty()) {
                RecentSearchesList(
                    searches = recentSearches,
                    focusedIndex = focusedIndex,
                    isFocused = isListFocused,
                    onRecentSearchSelect = onRecentSearchSelect,
                )
            } else {
                EmptyState(message = stringResource(R.string.ui_quick_menu_search_prompt))
            }
        } else if (results.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.ui_quick_menu_search_no_results, query)
            )
        } else {
            GameList(
                games = results,
                focusedIndex = focusedIndex,
                isFocused = isListFocused,
                onGameSelect = onGameSelect
            )
        }
    }
}

@Composable
private fun RandomContent(
    game: GameCardUi?,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (game == null) {
        EmptyState(message = stringResource(R.string.ui_quick_menu_empty_random))
        return
    }

    val shape = RoundedCornerShape(Dimens.radiusPanel)
    val borderModifier = if (isFocused) {
        Modifier.border(Dimens.borderMedium, MaterialTheme.colorScheme.primary, shape)
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxSize()
            .then(borderModifier)
            .background(
                if (isFocused) LocalArgosyTheme.current.focusAccent.copy(alpha = 0.15f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
                else MaterialTheme.colorScheme.surface,
                shape
            )
            .clip(shape)
            .clickableNoFocus(onClick = onClick)
            .padding(Dimens.spacingLg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingLg)
    ) {
        AsyncImage(
            model = rememberFileImageModel(game.coverPath),
            contentDescription = game.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(Dimens.radiusLg))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Text(
                text = game.platformName
                    ?: stringResource(R.string.ui_quick_menu_random_platform_unknown),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            Text(
                text = buildString {
                    game.year?.let { append(it) }
                    game.developer?.let {
                        if (isNotEmpty()) append(" | ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            game.genre?.let { genre ->
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                Text(
                    text = genre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                game.rating?.let { rating ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconSm + Dimens.borderMedium)
                        )
                        Text(
                            text = "${rating.toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (game.isDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(
                            R.string.ui_quick_menu_random_downloaded
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.iconMd)
                    )
                }
            }
        }
    }
}

@Composable
private fun ListContent(
    games: List<GameRowUi>,
    focusedIndex: Int,
    isFocused: Boolean,
    emptyMessage: String,
    onGameSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        EmptyState(message = emptyMessage)
        return
    }

    GameList(
        games = games,
        focusedIndex = focusedIndex,
        isFocused = isFocused,
        onGameSelect = onGameSelect,
        modifier = modifier
    )
}

@Composable
private fun GameList(
    games: List<GameRowUi>,
    focusedIndex: Int,
    isFocused: Boolean,
    onGameSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex) {
        if (focusedIndex in games.indices) {
            listState.animateScrollToItemCentered(focusedIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
            QuickMenuGameRow(
                game = game,
                isFocused = isFocused && index == focusedIndex,
                onClick = { onGameSelect(game.id) }
            )
        }
    }
}

@Composable
private fun AppsContent(
    apps: List<QuickMenuAppUi>,
    focusedIndex: Int,
    isFocused: Boolean,
    onAppLaunch: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onAddApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val totalCount = apps.size + 1

    LaunchedEffect(focusedIndex) {
        if (focusedIndex in 0 until totalCount) {
            val rowStart = (focusedIndex / QUICK_MENU_APP_GRID_COLUMNS) * QUICK_MENU_APP_GRID_COLUMNS
            gridState.animateScrollToItem(rowStart)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(QUICK_MENU_APP_GRID_COLUMNS),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(Dimens.spacingSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            QuickMenuAppCard(
                app = app,
                isFocused = isFocused && index == focusedIndex,
                onClick = { onAppLaunch(app.packageName) },
                onLongClick = { onRemoveApp(app.packageName) }
            )
        }
        item(key = "add_app") {
            AddAppTile(
                isFocused = isFocused && focusedIndex == apps.size,
                onClick = onAddApp
            )
        }
    }
}

@Composable
private fun AddAppTile(
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimens.radiusMd)

    Column(
        modifier = modifier
            .then(
                if (isFocused) {
                    Modifier.border(Dimens.borderMedium, MaterialTheme.colorScheme.primary, shape)
                } else Modifier
            )
            .clip(shape)
            .clickableNoFocus(onClick = onClick)
            .background(
                if (isFocused) LocalArgosyTheme.current.focusAccent.copy(alpha = 0.15f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
                else MaterialTheme.colorScheme.surface,
                shape
            )
            .padding(Dimens.radiusLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.ui_quick_menu_add_app),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.iconXl)
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSm))

        Text(
            text = stringResource(R.string.ui_quick_menu_add_app),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickMenuAppCard(
    app: QuickMenuAppUi,
    isFocused: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimens.radiusMd)

    Column(
        modifier = modifier
            .then(
                if (isFocused) {
                    Modifier.border(Dimens.borderMedium, MaterialTheme.colorScheme.primary, shape)
                } else Modifier
            )
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(
                if (isFocused) LocalArgosyTheme.current.focusAccent.copy(alpha = 0.15f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
                else MaterialTheme.colorScheme.surface,
                shape
            )
            .padding(Dimens.radiusLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = AppIconData(app.packageName),
            contentDescription = app.label,
            modifier = Modifier
                .size(Dimens.iconXl)
                .clip(RoundedCornerShape(Dimens.radiusMd))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.radiusMd)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSm))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentSearchesList(
    searches: List<String>,
    focusedIndex: Int,
    isFocused: Boolean,
    onRecentSearchSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex) {
        if (focusedIndex in searches.indices) {
            listState.animateScrollToItemCentered(focusedIndex)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.ui_quick_menu_recent_searches_heading),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimens.spacingSm)
        )

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
        ) {
            itemsIndexed(searches, key = { index, _ -> index }) { index, query ->
                RecentSearchRow(
                    query = query,
                    isFocused = isFocused && index == focusedIndex,
                    onRecentSearchSelect = onRecentSearchSelect
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    query: String,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    onRecentSearchSelect: (String) -> Unit
) {
    val shape = RoundedCornerShape(Dimens.radiusMd)
    val borderModifier = if (isFocused) {
        Modifier.border(Dimens.borderMedium, MaterialTheme.colorScheme.primary, shape)
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .background(
                if (isFocused) LocalArgosyTheme.current.focusAccent.copy(alpha = 0.15f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
                else MaterialTheme.colorScheme.surface,
                shape
            )
            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm)
            .clickableNoFocus(onClick = { onRecentSearchSelect(query) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = if (isFocused) lerp(LocalArgosyTheme.current.focusAccent, Color.White, 0.45f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.iconSm)
        )
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFocused) lerp(LocalArgosyTheme.current.focusAccent, Color.White, 0.45f)
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.spacingXl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickMenuAppPicker(
    installedApps: List<QuickMenuAppUi>,
    systemApps: List<QuickMenuAppUi>,
    hiddenApps: List<QuickMenuAppUi>,
    focusedIndex: Int,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalLauncherTheme.current.isDarkTheme
    val scrimColor = if (isDarkTheme) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.55f)

    val totalCount = installedApps.size + systemApps.size + hiddenApps.size

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(scrimColor)
            .clickableNoFocus(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(Dimens.modalWidth)
                .heightIn(max = maxHeight * 0.8f)
                .clip(RoundedCornerShape(Dimens.radiusPanel))
                .background(MaterialTheme.colorScheme.surface)
                .clickableNoFocus(enabled = false) {}
                .padding(vertical = Dimens.spacingSm)
        ) {
            Text(
                text = stringResource(R.string.ui_quick_menu_select_app),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = Dimens.radiusLg)
            )

            if (totalCount == 0) {
                Text(
                    text = stringResource(R.string.ui_quick_menu_empty_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Dimens.spacingLg)
                )
            } else {
                val listState = rememberLazyListState()

                LaunchedEffect(focusedIndex) {
                    if (focusedIndex in 0 until totalCount) {
                        val headersBefore = when {
                            focusedIndex < installedApps.size ->
                                if (installedApps.isNotEmpty()) 1 else 0
                            focusedIndex < installedApps.size + systemApps.size ->
                                (if (installedApps.isNotEmpty()) 1 else 0) +
                                    (if (systemApps.isNotEmpty()) 1 else 0)
                            else ->
                                (if (installedApps.isNotEmpty()) 1 else 0) +
                                    (if (systemApps.isNotEmpty()) 1 else 0) +
                                    (if (hiddenApps.isNotEmpty()) 1 else 0)
                        }
                        listState.animateScrollToItemCentered(focusedIndex + headersBefore)
                    }
                }

                val installedOffset = 0
                val systemOffset = installedApps.size
                val hiddenOffset = installedApps.size + systemApps.size

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
                ) {
                    if (installedApps.isNotEmpty()) {
                        item(key = "header_installed") {
                            AppPickerHeader(stringResource(R.string.ui_quick_menu_apps_section_installed))
                        }
                        itemsIndexed(installedApps, key = { _, app -> app.packageName }) { index, app ->
                            QuickMenuAppPickerRow(
                                app = app,
                                isFocused = installedOffset + index == focusedIndex,
                                onClick = { onSelect(app.packageName) }
                            )
                        }
                    }
                    if (systemApps.isNotEmpty()) {
                        item(key = "header_system") {
                            AppPickerHeader(stringResource(R.string.ui_quick_menu_apps_section_system))
                        }
                        itemsIndexed(systemApps, key = { _, app -> app.packageName }) { index, app ->
                            QuickMenuAppPickerRow(
                                app = app,
                                isFocused = systemOffset + index == focusedIndex,
                                onClick = { onSelect(app.packageName) }
                            )
                        }
                    }
                    if (hiddenApps.isNotEmpty()) {
                        item(key = "header_hidden") {
                            AppPickerHeader(stringResource(R.string.ui_quick_menu_apps_section_hidden))
                        }
                        itemsIndexed(hiddenApps, key = { _, app -> app.packageName }) { index, app ->
                            QuickMenuAppPickerRow(
                                app = app,
                                isFocused = hiddenOffset + index == focusedIndex,
                                onClick = { onSelect(app.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingXs)
    )
}

@Composable
private fun QuickMenuAppPickerRow(
    app: QuickMenuAppUi,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(Dimens.radiusMd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFocused) {
                    Modifier.border(Dimens.borderMedium, MaterialTheme.colorScheme.primary, shape)
                } else Modifier
            )
            .background(
                if (isFocused) LocalArgosyTheme.current.focusAccent.copy(alpha = 0.15f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
                else MaterialTheme.colorScheme.surface,
                shape
            )
            .clip(shape)
            .clickableNoFocus(onClick = onClick)
            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = AppIconData(app.packageName),
            contentDescription = app.label,
            modifier = Modifier
                .size(Dimens.iconMd)
                .clip(RoundedCornerShape(Dimens.radiusMd))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.width(Dimens.spacingMd))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFocused) lerp(LocalArgosyTheme.current.focusAccent, Color.White, 0.45f)
                else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
