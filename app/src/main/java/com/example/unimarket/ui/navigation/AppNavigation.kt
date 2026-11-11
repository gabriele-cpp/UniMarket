package com.example.unimarket.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.chat.ChatListScreen
import com.example.unimarket.ui.chat.ChatScreen
import com.example.unimarket.ui.chat.ChatViewModel
import com.example.unimarket.ui.detail.DetailScreen
import com.example.unimarket.ui.home.HomeScreen
import com.example.unimarket.ui.login.LoginScreen
import com.example.unimarket.ui.menu.MenuScreen
import com.example.unimarket.ui.myitems.MyItemsScreen
import com.example.unimarket.ui.post.PostScreen
import com.example.unimarket.ui.post.PostViewModel
import com.example.unimarket.ui.profile.ProfileScreen
import com.example.unimarket.ui.register.RegisterScreen
import com.example.unimarket.ui.search.SearchScreen

@Composable
fun AppNavigation(
    sessionViewModel: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val startDestination by sessionViewModel.startDestination.collectAsState()

    // Selama sesi masih diperiksa, tampilkan loading (startDestination NavHost tidak boleh berubah).
    val start = startDestination
    if (start == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = start
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigateClearingBackStack(Routes.HOME) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigateClearingBackStack(Routes.HOME) },
                onLoginClick = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onProductClick = { productId ->
                    navController.navigate(Routes.detail(productId))
                },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onPostClick = { navController.navigate(Routes.post()) },
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
                // Pembeli membuka chat dengan buyerId = dirinya sendiri.
                onChat = { buyerId ->
                    navController.navigate(Routes.chat(productId, buyerId))
                },
                onEdit = {
                    navController.navigate(Routes.post(productId))
                }
            )
        }

        composable(
            route = Routes.POST,
            arguments = listOf(
                // Opsional: -1 = barang baru. Dibaca PostViewModel lewat SavedStateHandle.
                navArgument(PostViewModel.PRODUCT_ID_ARG) {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            PostScreen(
                onBack = { navController.popBackStack() },
                onPostSuccess = { navController.popBackStack() }
            )
        }

        composable(Routes.MY_ITEMS) {
            MyItemsScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { productId -> navController.navigate(Routes.detail(productId)) },
                onEditClick = { productId -> navController.navigate(Routes.post(productId)) },
                onPostClick = { navController.navigate(Routes.post()) }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                // Dibaca ChatViewModel lewat SavedStateHandle.
                navArgument(ChatViewModel.PRODUCT_ID_ARG) { type = NavType.IntType },
                navArgument(ChatViewModel.BUYER_ID_ARG) { type = NavType.LongType }
            )
        ) {
            ChatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CHATS) {
            ChatListScreen(
                onBack = { navController.popBackStack() },
                onConversationClick = { productId, buyerId ->
                    navController.navigate(Routes.chat(productId, buyerId))
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = { navController.navigateClearingBackStack(Routes.LOGIN) },
                onMyItemsClick = { navController.navigate(Routes.MY_ITEMS) }
            )
        }

        composable(Routes.MENU) {
            MenuScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Hindari duplikasi layar yang sama di back stack
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onSignedOut = { navController.navigateClearingBackStack(Routes.LOGIN) }
            )
        }
    }
}

/** Pindah ke [route] dan hapus seluruh back stack (dipakai setelah login/daftar/logout). */
private fun NavHostController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
