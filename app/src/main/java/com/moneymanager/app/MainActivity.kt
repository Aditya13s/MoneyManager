package com.moneymanager.app

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moneymanager.shared.app.MoneyManagerApp
import com.moneymanager.shared.app.PlatformFileBridge
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var onCsvLoaded by remember { mutableStateOf<((String) -> Unit)?>(null) }
            var onCsvError by remember { mutableStateOf<((String) -> Unit)?>(null) }
            val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) {
                    onCsvError?.invoke("No file selected")
                    return@rememberLauncherForActivityResult
                }
                runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Unable to read selected file")
                }.onSuccess { text ->
                    onCsvLoaded?.invoke(text)
                }.onFailure { e ->
                    onCsvError?.invoke(e.message ?: "Unable to import CSV")
                }
            }

            MoneyManagerApp(
                fileBridge = object : PlatformFileBridge {
                    override fun importCsv(onLoaded: (String) -> Unit, onError: (String) -> Unit) {
                        onCsvLoaded = onLoaded
                        onCsvError = onError
                        csvImportLauncher.launch("*/*")
                    }

                    override fun exportCsv(content: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
                        runCatching {
                            val file = File(getExternalFilesDir(null), "transactions_${System.currentTimeMillis()}.csv")
                            FileOutputStream(file).bufferedWriter().use { it.write(content) }
                            file.absolutePath
                        }.onSuccess(onSuccess).onFailure { onError(it.message ?: "Unable to export CSV") }
                    }
                }
            )
        }
    }
}
