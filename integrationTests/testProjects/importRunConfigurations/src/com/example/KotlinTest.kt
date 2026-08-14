package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinTest {
    @Test
    fun test() {
        assertEquals(4, 2 + 2)
    }
}

// Shouldn't show gutter here
fun anotherRandomMethod() {}

// Shouldn't show gutter here
class RandomClass {}
