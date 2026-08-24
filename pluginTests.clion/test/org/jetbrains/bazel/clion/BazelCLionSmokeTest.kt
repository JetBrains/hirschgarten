package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.commons.LanguageClassService
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelCLionSmokeTest {

  private val project by clionBazelProjectFixture("import/mixed")

  @Test
  fun testCLionIsLoaded() {
    LanguageClassService.getInstance().fromName("cc").shouldNotBeNull()

    PluginManagerCore.getPluginSet()
      .findEnabledPlugin(PluginId.getId("org.jetbrains.plugins.clion.radler"))
      .shouldNotBeNull()

    assertNotNull(project.basePath, "project fixture failed to initialize")
    assertNotNull(LanguageEngine.INSTANCE_OR_NULL, "CLion language engine (Radler) is not registered")
  }
}
