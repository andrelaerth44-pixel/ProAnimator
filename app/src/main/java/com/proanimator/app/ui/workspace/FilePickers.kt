package com.proanimator.app.ui.workspace

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.io.FileOutputStream

data class FilePickLaunchers(
    val pickLottie: () -> Unit,
    val pickPan: () -> Unit,
    val createPan: (suggestedName: String) -> Unit,
    val createMp4: (suggestedName: String) -> Unit
)

@Composable
fun rememberFilePickLaunchers(
    context: Context,
    onLottieUri: (Uri) -> Unit,
    onPanUri: (Uri) -> Unit,
    onCreatePanUri: (Uri) -> Unit,
    onCreateMp4Uri: (Uri) -> Unit
): FilePickLaunchers {
    val lottieLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onLottieUri) }

    val panLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPanUri) }

    val createPanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(onCreatePanUri) }

    val createMp4Launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri -> uri?.let(onCreateMp4Uri) }

    return remember(lottieLauncher, panLauncher, createPanLauncher, createMp4Launcher) {
        FilePickLaunchers(
            pickLottie = {
                lottieLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            pickPan = {
                panLauncher.launch(arrayOf("*/*", "application/octet-stream"))
            },
            createPan = { name ->
                createPanLauncher.launch(if (name.endsWith(".pan")) name else "$name.pan")
            },
            createMp4 = { name ->
                createMp4Launcher.launch(if (name.endsWith(".mp4")) name else "$name.mp4")
            }
        )
    }
}

fun Context.copyUriToCache(uri: Uri, suffix: String): File? {
    return try {
        val out = File(cacheDir, "import_${System.currentTimeMillis()}$suffix")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        out
    } catch (_: Exception) {
        null
    }
}
