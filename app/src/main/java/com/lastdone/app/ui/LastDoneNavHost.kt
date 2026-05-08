package com.lastdone.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lastdone.app.ui.additem.AddItemPrefill
import com.lastdone.app.ui.additem.AddItemScreen
import com.lastdone.app.ui.home.HomeScreen
import com.lastdone.app.ui.itemdetail.ItemDetailRoute
import com.lastdone.app.ui.settings.SettingsRoute

private object Routes {
    const val Home = "home"
    const val AddItem = "addItem?name={name}&intervalDays={intervalDays}&icon={icon}"
    const val EditItem = "editItem/{itemId}"
    const val ItemDetail = "itemDetail/{itemId}"
    const val Settings = "settings"

    fun itemDetail(id: Long) = "itemDetail/$id"
    fun editItem(id: Long) = "editItem/$id"
    fun addItem(): String = "addItem?name=&intervalDays=0&icon="
    fun addItemPrefill(name: String, intervalDays: Int, icon: String): String {
        val n = Uri.encode(name)
        val ic = Uri.encode(icon)
        return "addItem?name=$n&intervalDays=$intervalDays&icon=$ic"
    }
}

@Composable
fun LastDoneNavHost(
    pendingItemId: Long? = null,
    onPendingHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingItemId) {
        if (pendingItemId != null) {
            navController.navigate(Routes.itemDetail(pendingItemId))
            onPendingHandled()
        }
    }

    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                onAddClick = { navController.navigate(Routes.addItem()) },
                onItemClick = { id -> navController.navigate(Routes.itemDetail(id)) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                onSuggestionClick = { suggestion ->
                    navController.navigate(
                        Routes.addItemPrefill(
                            name = suggestion.name,
                            intervalDays = suggestion.intervalDays,
                            icon = suggestion.icon
                        )
                    )
                }
            )
        }
        composable(
            route = Routes.AddItem,
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("intervalDays") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("icon") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val name = args?.getString("name").orEmpty()
            val intervalDays = args?.getInt("intervalDays") ?: 0
            val icon = args?.getString("icon").orEmpty()
            val prefill = if (name.isNotBlank()) {
                AddItemPrefill(name = name, intervalDays = intervalDays, icon = icon)
            } else null
            AddItemScreen(
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                prefill = prefill
            )
        }
        composable(
            route = Routes.EditItem,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments!!.getLong("itemId")
            AddItemScreen(
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                itemId = itemId
            )
        }
        composable(
            route = Routes.ItemDetail,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments!!.getLong("itemId")
            ItemDetailRoute(
                itemId = itemId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editItem(itemId)) }
            )
        }
        composable(Routes.Settings) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
