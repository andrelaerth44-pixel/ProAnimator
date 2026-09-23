package com.proanimator.app.ui.workspace

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.io.FileOutputStream

/**
 * SAF pickers for Lottie JSON and .pan projects.
 */
data class FilePickLaunchers(
    val pickLottie: () -> Unit,
    val pickPan: () -> Unit
)

@Composable
fun rememberFilePickLaunchers(
    context: Context,
    onLottieUri: (Uri) -> Unit,
    onPanUri: (Uri) -> Unit
): FilePickLaunchers {
    val lottieLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onLottieUri) }

    val panLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPanUri) }

    return remember(lottieLauncher, panLauncher) {
        FilePickLaunchers(
            pickLottie = {
                lottieLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            pickPan = {
                panLauncher.launch(arrayOf("*/*", "application/octet-stream"))
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
