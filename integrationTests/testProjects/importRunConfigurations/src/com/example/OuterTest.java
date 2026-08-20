package com.example;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class OuterTest {
  public static class NestedTest {
    @Test
    public void nestedTest() {
      assert(true);
    }

    @Test
    public void anotherNestedTest() {
      assert(true);
    }
  }
}
