package com.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CalculatorTest {
    @Test
    public void testAdd() {
        assertEquals(4, Calculator.add(2, 2));
    }

    @Test
    public void testMultiply() {
        assertEquals(4, Calculator.multiply(2, 2));
    }
}
