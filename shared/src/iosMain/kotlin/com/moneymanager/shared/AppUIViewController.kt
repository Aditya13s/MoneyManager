package com.moneymanager.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.moneymanager.shared.app.MoneyManagerApp
import com.moneymanager.shared.app.PlatformFileBridge
import platform.UIKit.UIViewController

private class IOSPlatformFileBridge(
    private val hostViewController: () -> UIViewController?
) : PlatformFileBridge {

    override fun importCsv(onLoaded: (String) -> Unit, onError: (String) -> Unit) {
        if (hostViewController() == null) return onError("Unable to open file picker")
        onError("CSV import is currently unavailable on iOS")
    }

    override fun exportCsv(content: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (hostViewController() == null) return onError("Unable to export file")
        onError("CSV export is currently unavailable on iOS")
    }
}

fun MainViewController(): UIViewController {
    var rootController: UIViewController? = null
    val fileBridge = IOSPlatformFileBridge { rootController }
    val composeController = ComposeUIViewController {
        MoneyManagerApp(fileBridge = fileBridge)
    }
    rootController = composeController
    return composeController
}
