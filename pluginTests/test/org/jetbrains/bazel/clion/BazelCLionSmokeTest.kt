package org.jetbrains.bazel.clion

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClassService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test

internal class BazelCLionSmokeTest: MockProjectBaseTest() {
  @Test
  fun testCLIonIsLoaded() {
    LanguageClassService.getInstance().fromExtension("cpp").shouldNotBeNull()

    PluginManagerCore.getPluginSet()
      .findEnabledPlugin(PluginId.getId("org.jetbrains.plugins.clion.radler"))
      .shouldNotBeNull()
  }
}
