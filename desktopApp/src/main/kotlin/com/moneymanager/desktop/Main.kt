package com.moneymanager.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.moneymanager.shared.SharedBootstrap

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MoneyManager") {
        App()
    }
}

@Composable
private fun App() {
    MaterialTheme {
        Text(SharedBootstrap().status())
    }
}
