package com.niranjan.medqueue.navigation

import androidx.compose.runtime.saveable.listSaver

/**
 * Detail and edit destinations carry a request **id**, not a whole
 * [com.niranjan.medqueue.data.local.RequestEntity]. Carrying the entity meant
 * the screen rendered a snapshot taken at navigation time, so an edit or a
 * status change left the previous screen showing stale values. The id is
 * resolved against the live list on every recomposition instead.
 */
sealed class Screen {
    object Splash      : Screen()
    object Onboarding  : Screen()
    object Home        : Screen()
    object RequestList : Screen()
    object Settings    : Screen()
    data class RequestDetail(val requestId: Int) : Screen()
    data class EditRequest(val requestId: Int)   : Screen()
}

/**
 * Lets the current destination survive rotation and process death.
 *
 * Restoring deliberately never returns to [Screen.Splash] — the splash is a
 * cold-start affordance, and replaying it on every rotation would be a bug.
 */
val ScreenSaver = listSaver<Screen, Any>(
    save = { screen ->
        when (screen) {
            Screen.Splash           -> listOf(KEY_SPLASH)
            Screen.Onboarding       -> listOf(KEY_ONBOARDING)
            Screen.Home             -> listOf(KEY_HOME)
            Screen.RequestList      -> listOf(KEY_LIST)
            Screen.Settings         -> listOf(KEY_SETTINGS)
            is Screen.RequestDetail -> listOf(KEY_DETAIL, screen.requestId)
            is Screen.EditRequest   -> listOf(KEY_EDIT, screen.requestId)
        }
    },
    restore = { saved ->
        when (saved.firstOrNull()) {
            // Onboarding does restore: dropping a half-finished setup back to
            // the queue would strand a shop with no name on its messages.
            KEY_ONBOARDING -> Screen.Onboarding
            KEY_HOME       -> Screen.Home
            KEY_SETTINGS   -> Screen.Settings
            KEY_DETAIL     -> Screen.RequestDetail(saved[1] as Int)
            KEY_EDIT       -> Screen.EditRequest(saved[1] as Int)
            else           -> Screen.RequestList
        }
    }
)

private const val KEY_SPLASH     = "splash"
private const val KEY_ONBOARDING = "onboarding"
private const val KEY_HOME       = "home"
private const val KEY_LIST       = "list"
private const val KEY_SETTINGS   = "settings"
private const val KEY_DETAIL     = "detail"
private const val KEY_EDIT       = "edit"

/**
 * Queue filter chips. The redesign drops the old "Today" chip — today's count
 * moved into the header subtitle, where it reads as context rather than as
 * another thing to tap.
 *
 * [URGENT] narrows to open emergencies. They already sort to the top of every
 * other view, but on a long queue the worker wants to see nothing else.
 */
enum class FilterTag { ALL, PENDING, URGENT, DELIVERED }
