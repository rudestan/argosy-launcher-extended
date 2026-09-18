package com.nendo.argosy.ui.quickmenu

import com.nendo.argosy.core.input.SoundType
import com.nendo.argosy.ui.input.InputHandler
import com.nendo.argosy.ui.input.InputResult

class QuickMenuInputHandler(
    private val viewModel: QuickMenuViewModel,
    private val onGameSelect: (Long) -> Unit,
    private val onLaunchApp: (String) -> Unit,
    private val onDismiss: () -> Unit
) : InputHandler {

    override fun onUp(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) {
            viewModel.moveAppPickerFocus(-1)
            return InputResult.HANDLED
        }

        if (state.contentFocused) {
            val isSearchWithInput = state.selectedOrb == QuickMenuOrb.SEARCH && state.searchInputFocused
            val isAtFirstItem = state.focusedContentIndex == 0 && !state.searchInputFocused
            val isRandom = state.selectedOrb == QuickMenuOrb.RANDOM

            when {
                isSearchWithInput || isRandom -> viewModel.exitContent()
                isAtFirstItem && state.selectedOrb == QuickMenuOrb.SEARCH -> viewModel.moveContentUp()
                isAtFirstItem -> viewModel.exitContent()
                else -> viewModel.moveContentUp()
            }
        }
        return InputResult.HANDLED
    }

    override fun onDown(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) {
            viewModel.moveAppPickerFocus(1)
            return InputResult.HANDLED
        }

        if (!state.contentFocused) {
            viewModel.enterContent()
        } else {
            viewModel.moveContentDown()
        }
        return InputResult.HANDLED
    }

    override fun onLeft(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) return InputResult.HANDLED

        if (state.contentFocused) {
            if (state.selectedOrb == QuickMenuOrb.APPS) {
                viewModel.moveContentLeft()
            }
        } else {
            viewModel.moveOrbLeft()
        }
        return InputResult.HANDLED
    }

    override fun onRight(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) return InputResult.HANDLED

        if (state.contentFocused) {
            if (state.selectedOrb == QuickMenuOrb.APPS) {
                viewModel.moveContentRight()
            }
        } else {
            viewModel.moveOrbRight()
        }
        return InputResult.HANDLED
    }

    override fun onConfirm(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) {
            viewModel.selectAppFromPicker()
            return InputResult.HANDLED
        }

        if (state.contentFocused) {
            if (state.selectedOrb == QuickMenuOrb.APPS) {
                if (viewModel.isAddAppTileFocused()) {
                    viewModel.openAppPicker()
                } else {
                    viewModel.getSelectedAppPackage()?.let { packageName ->
                        viewModel.hide()
                        onLaunchApp(packageName)
                    }
                }
            } else if (viewModel.isOnRecentSearches()) {
                viewModel.selectRecentSearch(state.focusedContentIndex)
            } else {
                viewModel.getSelectedGameId()?.let { gameId ->
                    viewModel.saveSearchQuery()
                    viewModel.hide()
                    onGameSelect(gameId)
                }
            }
        } else {
            viewModel.enterContent()
        }
        return InputResult.HANDLED
    }

    override fun onBack(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) {
            viewModel.closeAppPicker()
            return InputResult.HANDLED
        }

        if (state.contentFocused) {
            viewModel.exitContent()
        } else {
            viewModel.hide()
            onDismiss()
        }
        return InputResult.HANDLED
    }

    override fun onLeftStickClick(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) {
            viewModel.closeAppPicker()
            return InputResult.HANDLED
        }

        if (state.contentFocused) {
            viewModel.exitContent()
        } else {
            viewModel.hide()
            onDismiss()
        }
        return InputResult.HANDLED
    }

    override fun onPrevSection(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) return InputResult.HANDLED

        viewModel.moveOrbLeft()
        return InputResult.HANDLED
    }

    override fun onNextSection(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) return InputResult.HANDLED

        viewModel.moveOrbRight()
        return InputResult.HANDLED
    }

    override fun onMenu(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED
        return InputResult.HANDLED
    }

    override fun onSelect(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED
        return InputResult.HANDLED
    }

    override fun onSecondaryAction(): InputResult {
        val state = viewModel.uiState.value
        if (!state.isVisible) return InputResult.UNHANDLED

        if (state.showAppPicker) return InputResult.HANDLED

        if (state.contentFocused && state.selectedOrb == QuickMenuOrb.APPS && !viewModel.isAddAppTileFocused()) {
            viewModel.getSelectedAppPackage()?.let { packageName ->
                viewModel.requestRemoveApp(packageName)
                return InputResult.handled(SoundType.OPEN_MODAL)
            }
        }
        return InputResult.HANDLED
    }
}
