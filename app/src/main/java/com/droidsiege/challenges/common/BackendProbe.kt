package com.droidsiege.challenges.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.droidsiege.engine.BackendUrlStore
import com.droidsiege.engine.DEFAULT_BACKEND_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Probe button for talking to the real lab backend (`:backend` module, default
 * http://10.0.2.2:8080 from the emulator, editable in Settings). The request runs
 * on Dispatchers.IO and the backend's verbatim response is printed into the
 * console — the backend families' flags live server-side, exactly as they would
 * in a real engagement.
 */
@Composable
fun BackendProbeButton(
    label: String,
    request: (baseUrl: String) -> Request,
    onResult: (String) -> String = { it },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    val urlStore = remember { BackendUrlStore(context) }
    var output by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(DEFAULT_BACKEND_URL) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        urlStore.backendUrl.collect { baseUrl = it }
    }

    Button(
        onClick = {
            scope.launch(Dispatchers.IO) {
                val text =
                    try {
                        val effectiveBase = baseUrl.trimEnd('/')
                        val call = client.newCall(request(effectiveBase))
                        call.execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            "backend [$effectiveBase] -> HTTP ${response.code}\n$body"
                        }
                    } catch (boom: Exception) {
                        "backend unreachable (${boom.javaClass.simpleName}: ${boom.message})\n" +
                            "start it with: docker compose up --build, or run :backend from the IDE"
                    }
                output = withContext(Dispatchers.Main) { onResult(text) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
    ChallengeConsole(output)
}

fun backendGet(path: String): Request = Request.Builder().url(path).get().build()
