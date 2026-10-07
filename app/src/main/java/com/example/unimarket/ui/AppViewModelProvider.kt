package com.example.unimarket.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.unimarket.UniMarketApplication
import com.example.unimarket.ui.home.HomeViewModel
import com.example.unimarket.ui.post.PostViewModel

import com.example.unimarket.ui.search.SearchViewModel
import com.example.unimarket.ui.chat.ChatViewModel
import com.example.unimarket.ui.detail.DetailViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(uniMarketApplication().container.productRepository)
        }
        initializer {
            PostViewModel(uniMarketApplication().container.productRepository)
        }
        initializer {
            SearchViewModel(uniMarketApplication().container.productRepository)
        }
        initializer {
            DetailViewModel(uniMarketApplication().container.productRepository)
        }
        initializer {
            ChatViewModel(uniMarketApplication().container.productRepository)
        }
    }
}

fun CreationExtras.uniMarketApplication(): UniMarketApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UniMarketApplication)
