package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.commons.LanguageClassService
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Test

@CcTestApplication
class BazelCLionSmokeTest {

  private val project by clionBazelProjectFixture("import/mixed")

  @Test
  fun testCLionIsLoaded() {
    assertThat(LanguageClassService.getInstance().fromName("cc")).isNotNull()

    assertThat(PluginManagerCore.getPluginSet().findEnabledPlugin(PluginId.getId("org.jetbrains.plugins.clion.radler"))).isNotNull()
    assertThat(project.basePath).describedAs("project fixture failed to initialize").isNotNull()
    assertThat(LanguageEngine.INSTANCE_OR_NULL).describedAs("CLion language engine (Radler) is not registered").isNotNull()
  }
}
