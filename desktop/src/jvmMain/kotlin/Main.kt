import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.synapse.social.studioasinc.desktop.theme.SynapseTheme
import com.synapse.social.studioasinc.desktop.ui.DesktopMainScreen
import com.synapse.social.studioasinc.shared.di.storageModule
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import org.koin.core.context.startKoin
import io.github.aakira.napier.Napier

fun main() = application {
    try {
        startKoin {
            modules(storageModule)
        }

        // Ensure Supabase is initialized
        SupabaseClient.client
    } catch (e: Exception) {
        // Koin might already be started in dev reload, or Supabase missing config
        Napier.e("Failed to initialize backend services", e)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Synapse Desktop"
    ) {
        SynapseTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                DesktopMainScreen()
            }
        }
    }
}
