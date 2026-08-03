package com.moneymanager.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.moneymanager.shared.app.MoneyManagerApp
import com.moneymanager.shared.app.PlatformFileBridge
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

private class IOSPlatformFileBridge(
    private val hostViewController: () -> UIViewController?
) : NSObject(), PlatformFileBridge, UIDocumentPickerDelegateProtocol {
    private var onImportLoaded: ((String) -> Unit)? = null
    private var onImportError: ((String) -> Unit)? = null

    override fun importCsv(onLoaded: (String) -> Unit, onError: (String) -> Unit) {
        val host = hostViewController() ?: return onError("Unable to open file picker")
        onImportLoaded = onLoaded
        onImportError = onError
        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.comma-separated-values-text", "public.text"),
            inMode = UIDocumentPickerModeImport
        )
        picker.delegate = this
        host.presentViewController(picker, animated = true, completion = null)
    }

    override fun exportCsv(content: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val host = hostViewController() ?: return onError("Unable to export file")
        val outputPath = NSTemporaryDirectory() + "transactions_${kotlin.system.getTimeMillis()}.csv"
        val output = NSString.create(string = content)
        val written = output.writeToFile(outputPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!written) {
            onError("Unable to export CSV")
            return
        }

        val fileUrl = NSURL.fileURLWithPath(outputPath)
        val shareController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
        shareController.popoverPresentationController?.sourceView = host.view
        host.presentViewController(shareController, animated = true, completion = null)
        onSuccess(outputPath)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onImportError?.invoke("No file selected")
        clearImportCallbacks()
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val selectedUrl = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return onImportSelectionMissing()
        loadFromUrl(selectedUrl)
    }

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
        loadFromUrl(didPickDocumentAtURL)
    }

    private fun onImportSelectionMissing() {
        onImportError?.invoke("No file selected")
        clearImportCallbacks()
    }

    private fun loadFromUrl(selectedUrl: NSURL) {
        val text = NSString.stringWithContentsOfURL(selectedUrl, encoding = NSUTF8StringEncoding, error = null)?.toString()
        if (text == null) {
            onImportError?.invoke("Unable to read selected file")
        } else {
            onImportLoaded?.invoke(text)
        }
        clearImportCallbacks()
    }

    private fun clearImportCallbacks() {
        onImportLoaded = null
        onImportError = null
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
