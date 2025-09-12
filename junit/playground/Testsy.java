package org.jetbrains.bazel.junit.playground;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Playground JUnit 5 tests to demonstrate live streaming via TeamCity service messages.
 * Each test sleeps for a bit so you can observe the test tree updating in IntelliJ while running under Bazel.
 */
public class Testsy {

  private static void pause(long millis) {
    try {
      System.out.println("[playground] Sleeping " + millis + " ms on " + Thread.currentThread().getName());
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("fast pass 1")
  void fastPass1() {
    pause(300);
    assertTrue(true);
  }

  @Test
  @DisplayName("slow pass 2")
  void slowPass2() {
    pause(1200);
    assertEquals(2, 1 + 1);
  }

  @Test
  @DisplayName("medium fail 3")
  void mediumFail3() {
    pause(800);
    assertEquals("expected", "actual", "Intentional failure to showcase reporting");
  }

  @Test
  @DisplayName("slow pass 4")
  void slowPass4() {
    pause(1500);
    assertNotNull("ok");
  }

  @Test
  @DisplayName("fast fail 5")
  void fastFail5() {
    pause(200);
    fail("Deliberate failure (#5)");
  }

  @Test
  @DisplayName("pass with stdout 6")
  void passWithStdout6() {
    System.out.println("[playground] Hello from test 6");
    pause(500);
    assertTrue(3 > 2);
  }

  @Disabled("demonstrate ignored test")
  @Test
  @DisplayName("ignored 7")
  void ignored7() {
    pause(400);
  }

  @RepeatedTest(3)
  @DisplayName("repeated test 8 (x3)")
  void repeated8() {
    pause(300);
    assertTrue(true);
  }

  @Nested
  @DisplayName("Nested group")
  class NestedGroup {
    @Test
    @DisplayName("nested pass 9")
    void nestedPass9() {
      pause(600);
      assertTrue(true);
    }

    @Test
    @DisplayName("nested fail 10")
    void nestedFail10() {
      pause(700);
      assertTrue(false, "Deliberate nested failure (#10)");
    }

    @Test
    @DisplayName("nested pass 11")
    void nestedPass11() {
      pause(400);
      assertEquals("ok", "ok");
    }
  }

  @Test
  @DisplayName("pass 12")
  void pass12() {
    pause(500);
    assertTrue(true);
  }
}
