package com.example.unimarket.testutil

import com.example.unimarket.ui.util.ErrorMessages
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * ViewModel mencatat error lewat ErrorMessages.log(), yang di aplikasi memakai
 * android.util.Log. Di test JVM Log tidak tersedia, sehingga rule ini mengganti
 * pencatatnya dengan daftar di memori selama test berjalan, lalu memulihkannya.
 *
 * Bonus: test dapat memeriksa bahwa error benar-benar dicatat lewat [logged].
 */
class ErrorLogRule : TestWatcher() {
    val logged = mutableListOf<Throwable>()

    override fun starting(description: Description) {
        logged.clear()
        ErrorMessages.logger = { logged += it }
    }

    override fun finished(description: Description) {
        ErrorMessages.logger = ErrorMessages.logcatLogger
    }
}
