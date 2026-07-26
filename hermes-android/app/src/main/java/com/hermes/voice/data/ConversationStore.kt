package com.hermes.voice.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Conversation history, persisted as a single JSON file in app-private storage.
 * Deliberately simple: a personal agent client holds tens of conversations, not
 * millions, so a file plus an in-memory [StateFlow] beats a database here.
 */
class ConversationStore(context: Context) {

    private val file = File(context.filesDir, "conversations.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val writeLock = Mutex()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        val loaded = runCatching {
            if (!file.exists()) emptyList()
            else json.decodeFromString<List<Conversation>>(file.readText())
        }.getOrDefault(emptyList())
        _conversations.value = loaded.sortedByDescending { it.updatedAt }
    }

    fun get(id: String?): Conversation? = _conversations.value.firstOrNull { it.id == id }

    suspend fun upsert(conversation: Conversation) {
        val titled = if (conversation.title == DEFAULT_TITLE) {
            conversation.copy(title = conversation.derivedTitle())
        } else {
            conversation
        }
        val next = _conversations.value.filterNot { it.id == titled.id } + titled
        _conversations.value = next.sortedByDescending { it.updatedAt }
        persist()
    }

    suspend fun delete(id: String) {
        _conversations.value = _conversations.value.filterNot { it.id == id }
        persist()
    }

    suspend fun rename(id: String, title: String) {
        _conversations.value = _conversations.value.map {
            if (it.id == id) it.copy(title = title) else it
        }
        persist()
    }

    suspend fun clearAll() {
        _conversations.value = emptyList()
        persist()
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        writeLock.withLock {
            runCatching {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json.encodeToString(_conversations.value))
                tmp.renameTo(file)
            }
        }
        Unit
    }

    companion object {
        const val DEFAULT_TITLE = "New conversation"
    }
}
