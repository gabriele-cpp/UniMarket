package com.example.unimarket.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail/{productId}"
    /** Argumen opsional: tanpa productId = barang baru, dengan productId = edit barang. */
    const val POST = "post?productId={productId}"
    const val MY_ITEMS = "my_items"
    const val CHAT = "chat/{productId}/{buyerId}"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val MENU = "menu"

    fun detail(productId: Int) = "detail/$productId"
    /** Satu percakapan = barang + pembeli. */
    fun chat(productId: Int, buyerId: Long) = "chat/$productId/$buyerId"
    fun post(productId: Int? = null) = if (productId == null) "post" else "post?productId=$productId"
}
