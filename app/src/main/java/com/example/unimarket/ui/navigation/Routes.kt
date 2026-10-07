package com.example.unimarket.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail/{productId}"
    const val POST = "post"
    const val CHAT = "chat/{productId}"
    const val PROFILE = "profile"
    const val MENU = "menu"

    fun detail(productId: Int) = "detail/$productId"
    fun chat(productId: Int) = "chat/$productId"
}
