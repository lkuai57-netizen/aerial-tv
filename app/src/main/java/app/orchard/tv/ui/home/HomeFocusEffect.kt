package app.orchard.tv.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/** One owner for entry and overlay restoration, so an old restore cannot undo entry focus. */
@Composable
internal fun HomeFocusEffect(
    ready: Boolean,
    entry: Long,
    resetToFirst: Boolean,
    preferredKey: () -> String?,
    focus: (String) -> Boolean,
    restore: (Boolean) -> Unit,
    onEntryFocused: () -> Unit,
    onResetHandled: () -> Unit,
): Boolean {
    var handledEntry by remember { mutableStateOf<Long?>(null) }
    val preferredNow by rememberUpdatedState(preferredKey)
    val focusNow by rememberUpdatedState(focus)
    val restoreNow by rememberUpdatedState(restore)
    val focusedNow by rememberUpdatedState(onEntryFocused)
    val resetNow by rememberUpdatedState(onResetHandled)
    val pending = handledEntry != entry || resetToFirst
    LaunchedEffect(ready, entry, resetToFirst) {
        if (!ready) return@LaunchedEffect
        withFrameNanos { }
        if (pending) {
            val key = preferredNow()
            if (key != null) {
                // Wait for a just-created tile to attach. Never restore the old tile over this one.
                var focused = focusNow(key)
                repeat(3) {
                    if (!focused) { withFrameNanos { }; focused = focusNow(key) }
                }
                if (!focused) return@LaunchedEffect
                focusedNow()
            } else {
                restoreNow(resetToFirst)
            }
            handledEntry = entry
            if (resetToFirst) resetNow()
        } else {
            restoreNow(false)
        }
    }
    return pending
}
