package com.lastdone.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lastdone.app.ui.additem.AddItemScreen
import com.lastdone.app.ui.home.HomeScreen
import com.lastdone.app.ui.itemdetail.ItemDetailRoute
import com.lastdone.app.ui.itemdetail.ItemDetailScreen
import com.lastdone.app.ui.itemdetail.sampleItemDetailUi
import com.lastdone.app.ui.settings.SettingsRoute

private object Routes {
    const val Home = "home"
    const val AddItem = "addItem"
    const val EditItem = "editItem/{itemId}"
    const val ItemDetail = "itemDetail/{itemId}"
    const val Settings = "settings"
    const val DesignItemDetail = "designItemDetail"

    fun itemDetail(id: Long) = "itemDetail/$id"
    fun editItem(id: Long) = "editItem/$id"
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
                onAddClick = { navController.navigate(Routes.AddItem) },
                onItemClick = { id -> navController.navigate(Routes.itemDetail(id)) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                onDesignDetailClick = { navController.navigate(Routes.DesignItemDetail) }
            )
        }
        composable(Routes.AddItem) {
            AddItemScreen(
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
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
        composable(Routes.DesignItemDetail) {
            ItemDetailScreen(
                ui = sampleItemDetailUi,
                onBack = { navController.popBackStack() },
                onMarkDone = {}
            )
        }
    }
}
