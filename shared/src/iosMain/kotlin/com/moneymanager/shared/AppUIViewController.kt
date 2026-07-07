package com.moneymanager.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.moneymanager.shared.app.MoneyManagerApp
import com.moneymanager.shared.app.PlatformFileBridge
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    MoneyManagerApp(
        fileBridge = object : PlatformFileBridge {
            override fun importCsv(onLoaded: (String) -> Unit, onError: (String) -> Unit) {
                onError("CSV import picker is not yet available in iOS runner")
            }

            override fun exportCsv(content: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
                onError("CSV export file picker is not yet available in iOS runner")
            }
        }
    )
}
