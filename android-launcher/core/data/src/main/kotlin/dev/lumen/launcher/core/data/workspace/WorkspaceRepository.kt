package dev.lumen.launcher.core.data.workspace

import dev.lumen.launcher.core.data.db.GridDao
import dev.lumen.launcher.core.data.db.WorkspaceMetaEntity
import dev.lumen.launcher.core.data.db.toEntities
import dev.lumen.launcher.core.data.db.toWorkspace
import dev.lumen.launcher.core.data.model.WorkspaceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The persisted home layout.
 *
 * Two requirements shape this class. §5: the layout must be drawable on the *first* frame after
 * process death, so [preload] runs from `Application.onCreate` on a background thread and [state]
 * publishes as soon as the read lands. And drag must be frame-perfect, so [mutate] applies to the
 * in-memory state synchronously and persists through a conflated channel — a 120Hz drag writes the
 * database a handful of times, not once per frame, and the last write always lands.
 */
@Singleton
class WorkspaceRepository @Inject constructor(
    private val dao: GridDao,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** Conflated: only the newest pending layout is ever written. */
    private val writes = Channel<WorkspaceState>(Channel.CONFLATED)

    init {
        scope.launch {
            for (next in writes) {
                runCatching {
                    dao.replaceAll(next.toEntities(), WorkspaceMetaEntity(0, next.pageCount, next.seeded))
                }
            }
        }
    }

    /** Called once from Application.onCreate; safe to call again. */
    fun preload() {
        if (_ready.value) return
        scope.launch {
            withContext(Dispatchers.IO) {
                val loaded = runCatching { dao.items().toWorkspace(dao.meta()) }
                    .getOrDefault(WorkspaceState())
                // A mutation that raced the preload wins; never clobber user action with disk state.
                _state.compareAndSet(WorkspaceState(), loaded)
                _ready.value = true
            }
        }
    }

    fun mutate(transform: (WorkspaceState) -> WorkspaceState) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        writes.trySend(next)
    }

    /** Replaces everything — first-run seeding and (later) backup restore. */
    fun replace(state: WorkspaceState) {
        _state.value = state
        writes.trySend(state)
    }
}
