package com.example.unimarket.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * viewModelScope berjalan di Dispatchers.Main (main thread Android), yang TIDAK
 * ada di JVM laptop. Rule ini memasang dispatcher uji sebelum setiap test dan
 * melepasnya sesudahnya.
 *
 * Karena Main sekarang TestDispatcher, runTest { } otomatis memakai jam virtual
 * yang sama: delay()/debounce() di ViewModel dapat dimajukan dengan
 * advanceTimeBy(...) tanpa menunggu sungguhan.
 *
 * Berkas ini dapat disalin APA ADANYA; cukup ganti baris package.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
