package utils

import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.utils.StringUtils
import org.junit.jupiter.api.Test

class StringUtilsTest {
  @Test
  fun `should return the hashed string with n character`() {
    val input = "Hello world"
    val expectedHashedStringLength = 5
    val actualHashedString = StringUtils.md5Hash(input, expectedHashedStringLength)
    actualHashedString.length shouldBe expectedHashedStringLength
  }
}
