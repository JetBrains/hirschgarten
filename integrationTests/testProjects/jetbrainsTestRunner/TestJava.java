import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Playground JUnit 5 tests to demonstrate live streaming via TeamCity service messages.
 * Each test sleeps for a bit so you can observe the test tree updating in IntelliJ while running under Bazel.
 */
public class TestJava {
  @ParameterizedTest
  @ValueSource(classes = {String.class, Comparable.class}) // six numbers
  void testWithClasses(Class<Comparable> myClass) {
    assertTrue(myClass.getSimpleName().equals("String"));
  }

  @Test
  @DisplayName("medium fail 3")
  void mediumFail3() {
    pause(800);
    assertEquals("expected", "actual", "Intentional failure to showcase reporting");
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
  }

  private static void pause(long millis) {
    try {
      System.out.println("[playground] Sleeping " + millis + " ms on " + Thread.currentThread().getName());
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
