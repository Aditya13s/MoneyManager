package com.moneymanager.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.moneymanager.shared.app.MoneyManagerApp
import com.moneymanager.shared.app.PlatformFileBridge
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MoneyManager") {
        MoneyManagerApp(
            fileBridge = object : PlatformFileBridge {
                override fun importCsv(onLoaded: (String) -> Unit, onError: (String) -> Unit) {
                    val dialog = FileDialog(null as Frame?, "Select CSV file", FileDialog.LOAD)
                    dialog.isVisible = true
                    val fileName = dialog.file ?: return onError("No file selected")
                    runCatching {
                        File(dialog.directory, fileName).readText()
                    }.onSuccess(onLoaded).onFailure { onError(it.message ?: "Unable to read file") }
                }

                override fun exportCsv(content: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
                    val dialog = FileDialog(null as Frame?, "Save CSV file", FileDialog.SAVE)
                    dialog.file = "transactions.csv"
                    dialog.isVisible = true
                    val fileName = dialog.file ?: return onError("Export canceled")
                    runCatching {
                        val output = File(dialog.directory, fileName)
                        output.writeText(content)
                        output.absolutePath
                    }.onSuccess(onSuccess).onFailure { onError(it.message ?: "Unable to export file") }
                }
            }
        )
    }
}
