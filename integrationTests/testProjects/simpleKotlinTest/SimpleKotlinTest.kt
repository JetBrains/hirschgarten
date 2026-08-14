import org.junit.jupiter.api.Test
import io.kotest.matchers.shouldBe

class SimpleKotlinTest {
    @Test
    fun `trivial test`() {
        1+1 shouldBe 2
    }
    @Test
    fun `another trivial test`() {
        1+2 shouldBe 3
    }
}