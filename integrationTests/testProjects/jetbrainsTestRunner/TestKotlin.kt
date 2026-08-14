import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Playground JUnit 5 tests to demonstrate live streaming via TeamCity service messages.
 * Each test sleeps for a bit so you can observe the test tree updating in IntelliJ while running under Bazel.
 */
class TestKotlin {
  @Test
  fun ` interesting#test `() {
    println("[playground] Hello from interesting test")
    pause(500)
    Assertions.assertTrue(3 > 2)
  }


  @ParameterizedTest
  @ValueSource(ints = [1, 3, 5, 15, Int.MAX_VALUE])
  fun isOddShouldReturnTrueForOddNumbers(number: Int) {
    Assertions.assertEquals(1, number % 2)
  }

  companion object {
    private fun pause(millis: Long) {
      try {
        println("[playground] Sleeping " + millis + " ms on " + Thread.currentThread().getName())
        Thread.sleep(millis)
      }
      catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw RuntimeException(e)
      }
    }
  }
}
