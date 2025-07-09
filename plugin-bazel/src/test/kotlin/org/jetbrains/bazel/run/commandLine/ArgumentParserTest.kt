package org.jetbrains.bazel.run.commandLine

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ArgumentParserTest {
  @Test
  fun `parse commoner`() {
    val args = transformProgramArguments("""-s Suite -t "my test" """)
    args shouldBe listOf("-s", "Suite", "-t", "my test")
  }
  
  @Test
  fun `parse simpl`() {
    val args = transformProgramArguments("""-a A -b B """)
    args shouldBe listOf("-a", "A", "-b", "B")
  }
  
  @Test
  fun `parse escape`() {
    val args = transformProgramArguments("""-a A -b "B" """)
    args shouldBe listOf("-a", "A", "-b", "B")
  }

  @Test
  fun `parse empty string`() {
    val args = transformProgramArguments("")
    args shouldBe emptyList()
  }

  @Test
  fun `parse multiple quoted arguments`() {
    val args = transformProgramArguments("""--name "John Doe" --message "Hello World" """)
    args shouldBe listOf("--name", "John Doe", "--message", "Hello World")
  }

  @Test
  fun `parse mixed quoted and unquoted`() {
    val args = transformProgramArguments("""--flag1 value1 --flag2 "value 2" --flag3 value3""")
    args shouldBe listOf("--flag1", "value1", "--flag2", "value 2", "--flag3", "value3")
  }

  @Test
  fun `parse arguments with spaces`() {
    val args = transformProgramArguments("""  -x   test1    -y   "test 2"   """)
    args shouldBe listOf("-x", "test1", "-y", "test 2")
  }


  @Test
  fun `parse unclosed quotes at end`() {
    val args = transformProgramArguments("""-a value -b "unclosed""")
    args shouldBe listOf("-a", "value", "-b", "unclosed")
  }

  @Test
  fun `parse unclosed quotes in middle`() {
    val args = transformProgramArguments("""-a "unclosed -b value""")
    args shouldBe listOf("-a", "unclosed -b value")
  }

  @Test
  fun `parse with quote escaped`() {
    val args = transformProgramArguments("""-a "a \"b" """)
    args shouldBe listOf("-a", "a \"b")
  }

  @Test
  fun `parse illegal escape`() {
    val args = transformProgramArguments("""-a "a \""")
    args shouldBe listOf("-a", "a \\")
  }

}
