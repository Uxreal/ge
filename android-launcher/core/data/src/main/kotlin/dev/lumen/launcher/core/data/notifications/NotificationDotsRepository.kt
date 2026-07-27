package dev.lumen.launcher.core.data.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D43: which packages currently deserve a notification dot. The Capsule's notification listener
 * (the same consent token that powers media) writes here; home, dock and drawer icons read.
 *
 * Lives in `:core:data` because features never import each other (§2): the listener service is a
 * `:feature:capsule` citizen, the dots render in `:feature:home` and `:feature:drawer`, and this
 * flow is the only thing they share. In-memory only — a dot is live state, not a record; if the
 * process dies the listener repopulates it on reconnect, and nothing was ever written to disk.
 */
@Singleton
class NotificationDotsRepository @Inject constructor() {

    private val _packages = MutableStateFlow<Set<String>>(emptySet())

    /** Packages with at least one dot-worthy notification. Empty when access is off. */
    val packages: StateFlow<Set<String>> = _packages

    fun update(packages: Set<String>) {
        _packages.value = packages
    }

    companion object {
        /**
         * The dot policy, pure so it is testable: ongoing notifications never earn a dot. A media
         * session, a navigation, a foreground-service "running" banner — none of those are unread
         * news; a dot the user can never clear reads as broken. Everything else counts once —
         * three messages and one message are the same dot.
         */
        fun dotWorthy(active: List<Pair<String, Boolean>>): Set<String> =
            active.filterNot { (_, ongoing) -> ongoing }.map { (pkg, _) -> pkg }.toSet()
    }
}
