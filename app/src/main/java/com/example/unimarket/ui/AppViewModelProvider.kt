package com.example.unimarket.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import com.example.unimarket.ui.myitems.MyItemsViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.unimarket.AppContainer
import com.example.unimarket.UniMarketApplication
import com.example.unimarket.ui.chat.ChatListViewModel
import com.example.unimarket.ui.chat.ChatViewModel
import com.example.unimarket.ui.detail.DetailViewModel
import com.example.unimarket.ui.home.HomeViewModel
import com.example.unimarket.ui.login.LoginViewModel
import com.example.unimarket.ui.menu.MenuViewModel
import com.example.unimarket.ui.navigation.SessionViewModel
import com.example.unimarket.ui.post.PostViewModel
import com.example.unimarket.ui.profile.ProfileViewModel
import com.example.unimarket.ui.register.RegisterViewModel
import com.example.unimarket.ui.search.SearchViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val c = container()
            SessionViewModel(c.sessionRepository, c.userRepository)
        }
        initializer {
            val c = container()
            LoginViewModel(c.userRepository, c.sessionRepository)
        }
        initializer {
            val c = container()
            RegisterViewModel(c.userRepository, c.sessionRepository)
        }
        initializer {
            val c = container()
            HomeViewModel(c.productRepository, c.categoryRepository)
        }
        initializer {
            val c = container()
            // SavedStateHandle membawa argumen navigasi "productId" (mode edit).
            PostViewModel(createSavedStateHandle(), c.productRepository, c.categoryRepository, c.sessionRepository)
        }
        initializer {
            val c = container()
            SearchViewModel(c.productRepository, c.categoryRepository)
        }
        initializer {
            val c = container()
            DetailViewModel(c.productRepository, c.sessionRepository)
        }
        initializer {
            val c = container()
            MyItemsViewModel(c.productRepository, c.sessionRepository)
        }
        initializer {
            val c = container()
            // SavedStateHandle membawa argumen navigasi "productId" dan "buyerId".
            ChatViewModel(
                createSavedStateHandle(),
                c.productRepository,
                c.userRepository,
                c.messageRepository,
                c.sessionRepository
            )
        }
        initializer {
            val c = container()
            ChatListViewModel(c.messageRepository, c.sessionRepository)
        }
        initializer {
            val c = container()
            ProfileViewModel(c.sessionRepository, c.userRepository, c.productRepository)
        }
        initializer {
            MenuViewModel(container().sessionRepository)
        }
    }
}

fun CreationExtras.uniMarketApplication(): UniMarketApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UniMarketApplication)

private fun CreationExtras.container(): AppContainer = uniMarketApplication().container
