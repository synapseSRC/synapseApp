import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.synapse.social.studioasinc.desktop.theme.SynapseTheme
import com.synapse.social.studioasinc.desktop.ui.DesktopMainScreen
import com.synapse.social.studioasinc.desktop.ui.LoginScreen
import com.synapse.social.studioasinc.shared.di.storageModule
import com.synapse.social.studioasinc.shared.di.presenceModule
import com.synapse.social.studioasinc.desktop.di.desktopModule
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import org.koin.core.context.startKoin
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject

fun main() = application {
    val koin = remember {
        try {
            startKoin {
                modules(storageModule, presenceModule, desktopModule)
            }.koin
        } catch (e: Exception) {
            Napier.e("Failed to initialize Koin", e)
            null
        }
    }

    LaunchedEffect(Unit) {
        try {
            // Ensure Supabase is initialized
            SupabaseClient.client
        } catch (e: Exception) {
            Napier.e("Failed to initialize Supabase client", e)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Synapse Desktop",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        SynapseTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(
    authRepository: AuthRepository = koinInject()
) {
    val sessionStatus by authRepository.sessionStatus.collectAsState(AuthSessionStatus.INITIALIZING)

    when (sessionStatus) {
        AuthSessionStatus.AUTHENTICATED -> {
            DesktopMainScreen()
        }
        AuthSessionStatus.INITIALIZING, AuthSessionStatus.REFRESHING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            LoginScreen()
        }
    }
}
