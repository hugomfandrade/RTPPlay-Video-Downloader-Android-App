import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import kotlinx.coroutines.*
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

@Composable
@Preview
fun App() {
    var url by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter URL") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                downloading = true
                scope.launch {
                    for (i in 1..100) {
                        delay(50)
                        progress = i / 100f
                    }
                    downloading = false
                }
            }) {
                Text("Download")
            }

            if (downloading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = progress)
            }
        }
    }
}
