package com.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FlagPrecedenceTest {
    @Test
    public void testFlagPrecedence() {
        assertEquals("build_flags", System.getProperty("precedence.a", "unset"));
        assertEquals("build_flags", System.getProperty("precedence.b", "unset"));
    }
}
