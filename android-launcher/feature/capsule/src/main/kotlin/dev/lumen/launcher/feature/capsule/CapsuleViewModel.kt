package dev.lumen.launcher.feature.capsule

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * The UI's view of the Capsule. Thin on purpose: the deck belongs to the process, not to an
 * Activity, so [CapsuleController] outlives this and the ViewModel only forwards intent.
 */
@HiltViewModel
class CapsuleViewModel @Inject constructor(
    private val controller: CapsuleController,
) : ViewModel() {

    val deck: StateFlow<CapsuleDeck> = controller.deck

    fun pinFront(dedupeKey: String) = controller.pinFront(dedupeKey)

    fun dismiss(card: CapsuleCard) = controller.dismiss(card)

    fun refreshMedia() = controller.refreshMedia()

    fun mediaStatus(): String = controller.mediaStatus()

    fun pushTestCard() = controller.pushTest()

    private val _geometryDesc = MutableStateFlow("Not resolved yet.")
    val geometryDesc: StateFlow<String> = _geometryDesc.asStateFlow()

    fun reportGeometry(description: String) {
        _geometryDesc.value = description
    }
}
