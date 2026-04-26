import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import com.synapse.social.studioasinc.shared.di.storageModule
import com.synapse.social.studioasinc.web.di.webModule
import com.synapse.social.studioasinc.web.ui.WebAuthScreen
import com.synapse.social.studioasinc.web.ui.WebFeedScreen
import org.koin.core.context.startKoin

enum class WebScreen {
    Landing, Login, Feed
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(storageModule, webModule)
    }

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                var currentScreen by remember { mutableStateOf(WebScreen.Landing) }

                when (currentScreen) {
                    WebScreen.Landing -> WebPremiumLandingPage(onGetStarted = { currentScreen = WebScreen.Login })
                    WebScreen.Login -> WebAuthScreen(onLoginSuccess = { currentScreen = WebScreen.Feed })
                    WebScreen.Feed -> WebFeedScreen()
                }
            }
        }
    }
}

@Composable
fun WebPremiumLandingPage(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Synapse Web",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Experience premium social networking right in your browser.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Status: Online", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onGetStarted) {
                    Text("Get Started")
                }
            }
        }
    }
}
