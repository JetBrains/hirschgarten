package org.jetbrains.bazel.run.config

import org.jdom.Element
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@BazelTestApplication
class BazelRunConfigurationTest {
  private val myFixture by bazelCodeInsightFixture()

  @ParameterizedTest
  @ValueSource(strings = ["//app:bin", "  //some package:first   @repo//other package:second   //some package:first  "])
  fun `restore target text after loading a configuration`(text: String) {
    val configuration = createConfiguration()
    configuration.updateTargets(text, runHandlerProvider())
    val element = Element("configuration")
    configuration.writeExternal(element)

    val restored = createConfiguration()
    restored.readExternal(element)

    assertEquals(configuration.targets, restored.targets)
    assertEquals(configuration.targets.joinToString(" ") { it.toString() }, restored.targetListString)
    restored.updateTargets(restored.targetListString)
    assertEquals(configuration.targets, restored.targets)
  }

  @Test
  fun `update target text when setting a run provider`() {
    val configuration = createConfiguration()
    val targets = listOf(Label.parse("//app:bin"))

    configuration.updateRunProvider(targets, runHandlerProvider())

    assertEquals(targets, configuration.targets)
    assertEquals("@//app:bin", configuration.targetListString)
  }

  @ParameterizedTest
  @ValueSource(strings = ["//app:bin /", "//app:bin @repo", "//...:bin"])
  fun `invalid target text leaves the configuration unchanged`(text: String) {
    val configuration = createConfiguration()
    val originalText = "  //app:original  "
    configuration.updateTargets(originalText, runHandlerProvider())
    val targets = configuration.targets
    val handler = configuration.handler

    repeat(2) {
      assertFailsWith<IllegalArgumentException> { configuration.updateTargets(text) }
      assertEquals(originalText, configuration.targetListString)
      assertEquals(targets, configuration.targets)
      assertSame(handler, configuration.handler)
    }
  }

  private fun createConfiguration(): BazelRunConfiguration =
    BazelRunConfigurationType().createTemplateConfiguration(myFixture.project) as BazelRunConfiguration

  private fun runHandlerProvider(): RunHandlerProvider =
    requireNotNull(RunHandlerProvider.findRunHandlerProvider("GenericRunHandlerProvider"))
}
