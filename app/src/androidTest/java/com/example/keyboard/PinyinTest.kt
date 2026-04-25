package com.example.keyboard

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import android.util.Log

@RunWith(AndroidJUnit4::class)
class PinyinTest {
    @Test
    fun testPinyinEngine() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        PinyinEngine.init(appContext)
        Thread.sleep(3000)
        val res = PinyinEngine.fetchCandidates("sm")
        Log.d("TEST_PINYIN", "SM_RES: " + res)
    }
}
