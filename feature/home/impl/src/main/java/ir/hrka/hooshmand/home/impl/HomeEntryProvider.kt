package ir.hrka.hooshmand.home.impl

import androidx.compose.material3.Text
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.home.api.HomeNavKey
import ir.hrka.hooshmand.navigation.Navigator

fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> {
        Text(text = "Home Screen")
    }
}
