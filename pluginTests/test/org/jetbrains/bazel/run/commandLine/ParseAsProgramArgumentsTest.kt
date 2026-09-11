package org.jetbrains.bazel.run.commandLine

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ParseAsProgramArgumentsTest {
  @Test
  fun `parse commoner`() {
    val args = parseAsProgramArguments("""-s Suite -t "my test" """)
    args shouldBe listOf("-s", "Suite", "-t", "my test")
  }

  @Test
  fun `parse simpl`() {
    val args = parseAsProgramArguments("""-a A -b B """)
    args shouldBe listOf("-a", "A", "-b", "B")
  }

  @Test
  fun `parse escape`() {
    val args = parseAsProgramArguments("""-a A -b "B" """)
    args shouldBe listOf("-a", "A", "-b", "B")
  }

  @Test
  fun `parse empty string`() {
    val args = parseAsProgramArguments("")
    args shouldBe emptyList()
  }

  @Test
  fun `parse null`() {
    val args = parseAsProgramArguments(null)
    args shouldBe emptyList()
  }

  @Test
  fun `parse multiple quoted arguments`() {
    val args = parseAsProgramArguments("""--name "John Doe" --message "Hello World" """)
    args shouldBe listOf("--name", "John Doe", "--message", "Hello World")
  }

  @Test
  fun `parse mixed quoted and unquoted`() {
    val args = parseAsProgramArguments("""--flag1 value1 --flag2 "value 2" --flag3 value3""")
    args shouldBe listOf("--flag1", "value1", "--flag2", "value 2", "--flag3", "value3")
  }

  @Test
  fun `parse arguments with spaces`() {
    val args = parseAsProgramArguments("""  -x   test1    -y   "test 2"   """)
    args shouldBe listOf("-x", "test1", "-y", "test 2")
  }

  @Test
  fun `parse unclosed quotes at end`() {
    val args = parseAsProgramArguments("""-a value -b "unclosed""")
    args shouldBe listOf("-a", "value", "-b", "unclosed")
  }

  @Test
  fun `parse unclosed quotes in middle`() {
    val args = parseAsProgramArguments("""-a "unclosed -b value""")
    args shouldBe listOf("-a", "unclosed -b value")
  }

  @Test
  fun `parse with quote escaped`() {
    val args = parseAsProgramArguments("""-a "a \"b" """)
    args shouldBe listOf("-a", "a \"b")
  }

  @Test
  fun `parse illegal escape`() {
    val args = parseAsProgramArguments("""-a "a \""")
    args shouldBe listOf("-a", "a \\")
  }

  @Test
  fun `parse quoted value after equals sign`() {
    val args = parseAsProgramArguments("""--some-flag="232323"""")
    args shouldBe listOf("--some-flag=232323")
  }

  @Test
  fun `parse quoted value with spaces after equals sign`() {
    val args = parseAsProgramArguments("""--copt="-O2 -g" --other""")
    args shouldBe listOf("--copt=-O2 -g", "--other")
  }

  @Test
  fun `parse single quoted value`() {
    val args = parseAsProgramArguments("""--test_filter='foo bar'""")
    args shouldBe listOf("--test_filter=foo bar")
  }

  @Test
  fun `parse empty quoted value`() {
    val args = parseAsProgramArguments("""--flag=""""")
    args shouldBe listOf("--flag=")
  }

  @Test
  fun `parse tab and newline separators`() {
    val args = parseAsProgramArguments("-a\tb\n-c")
    args shouldBe listOf("-a", "b", "-c")
  }
}
