package dev.lumen.launcher.ui.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import dev.lumen.launcher.data.prefs.GlassSurfaceId
import dev.lumen.launcher.system.SystemActions
import dev.lumen.launcher.ui.LocalBackdrop
import dev.lumen.launcher.ui.glass.GlassSurface

/**
 * Until Lumen is the default home app, none of this is visible to the user — Android keeps sending
 * HOME to whatever launcher is set. So when that is the case, say so plainly and offer the one tap
 * that fixes it.
 *
 * Deliberately not persisted: the banner is derived from the actual system state, so it disappears
 * the moment the user sets Lumen as home and comes back on its own if they ever switch away. A
 * dismissal only lasts for this session.
 */
@Composable
fun SetupBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val backdrop = LocalBackdrop.current
    var dismissed by remember { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }

    // Re-check on every resume: the user leaves to the system picker and comes straight back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefault = isDefaultHome(context)
    }

    val visible = !isDefault && !dismissed

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        val panel: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Make Lumen your home app",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Android is still sending Home to another launcher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { SystemActions.openHomeSettings(context) }) {
                    Text("Set up")
                }
                TextButton(onClick = { dismissed = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (backdrop != null) {
            GlassSurface(
                surface = GlassSurfaceId.TOOLBAR,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
            ) {
                panel()
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) { panel() }
        }
    }
}

/**
 * True when this package is the resolved HOME activity. When several launchers are installed and no
 * default has been chosen, Android resolves to the system resolver activity, which correctly reads
 * as "not the default" here.
 */
fun isDefaultHome(context: Context): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    resolved?.activityInfo?.packageName == context.packageName
}.getOrDefault(false)
