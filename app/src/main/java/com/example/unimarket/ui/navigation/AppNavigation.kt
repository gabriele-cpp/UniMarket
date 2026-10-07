package com.example.unimarket.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.unimarket.UniMarketApplication
import com.example.unimarket.ui.chat.ChatScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarket.ui.chat.ChatScreen
import com.example.unimarket.ui.detail.DetailScreen
import com.example.unimarket.ui.home.HomeScreen
import com.example.unimarket.ui.login.LoginScreen
import com.example.unimarket.ui.menu.MenuScreen
import com.example.unimarket.ui.post.PostScreen
import com.example.unimarket.ui.profile.ProfileScreen
import com.example.unimarket.ui.search.SearchScreen

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val userPreferences = remember { (context.applicationContext as UniMarketApplication).container.userPreferences }
    val navController = rememberNavController()

    val startDestination = if (userPreferences.isLoggedIn()) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onProductClick = { productId ->
                    navController.navigate(Routes.detail(productId))
                },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onPostClick = { navController.navigate(Routes.POST) },
                onMenuClick = { navController.navigate(Routes.MENU) },
                onProfileClick = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Routes.detail(productId))
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("productId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: 1
            DetailScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
                onChat = {
                    navController.navigate(Routes.chat(productId))
                }
            )
        }

        composable(Routes.POST) {
            PostScreen(
                onBack = { navController.popBackStack() },
                onPostSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("productId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: 1
            ChatScreen(
                productId = productId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    userPreferences.clearSession()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MENU) {
            MenuScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onSignOut = {
                    userPreferences.clearSession()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
