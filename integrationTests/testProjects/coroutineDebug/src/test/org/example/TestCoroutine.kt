package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class TestCoroutine {
    @Test
    fun testCoroutine() {
        runBlocking {
            launch(Dispatchers.Default) {
                delay(100)
                firstLevel()
                delay(100)
            }
        }
    }

    suspend fun firstLevel() {
        delay(100)
        secondLevel()
        delay(100)
    }

    suspend fun secondLevel() {
        delay(100)
        // breakpoint here
        println("secondLevel")
        delay(100)
    }
}