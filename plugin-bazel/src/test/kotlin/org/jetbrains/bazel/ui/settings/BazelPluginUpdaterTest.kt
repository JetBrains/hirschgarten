package org.jetbrains.bazel.ui.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BazelPluginUpdaterTest {
  /**
   * The test is currently disabled due to no easy way to get the plugin descriptor in test.
   * Re-enable this when there is already such mechanism.
   *
   * TODO: https://youtrack.jetbrains.com/issue/BAZEL-1389/Provide-a-way-to-write-unittests-with-the-plugins-installed
   */
  @Disabled("Enable when there is a way to get the plugin descriptor in test")
  @Test
  fun `patchPluginVersion should function normally`() {
    // given
    val bazelPluginDescriptor =
      BazelPluginUpdater.getPluginDescriptorForId(BAZEL_PLUGIN_ID)
        ?: error("Bazel plugin with ID `$BAZEL_PLUGIN_ID` not found")
    val currentVersion =
      bazelPluginDescriptor.version
        ?: error("Bazel plugin version not found")
    val patchVersion = "0.0.0"

    // when
    BazelPluginUpdater.patchPluginVersion(patchVersion, bazelPluginDescriptor)

    // then
    bazelPluginDescriptor.version shouldBe patchVersion

    // reset environment
    BazelPluginUpdater.patchPluginVersion(currentVersion, bazelPluginDescriptor)
    bazelPluginDescriptor.version shouldBe currentVersion
  }
}
