package com.example;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FailingTest {
    @Test
    public void testPasses() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testAnotherPasses() {
        assertEquals(4, 2 * 2);
    }

    @Test
    public void testFails() {
        assertEquals(5, 2 + 2);
    }

    @Test
    public void testAnotherFails() {
        assertEquals(5, 2 * 2);
    }
}
