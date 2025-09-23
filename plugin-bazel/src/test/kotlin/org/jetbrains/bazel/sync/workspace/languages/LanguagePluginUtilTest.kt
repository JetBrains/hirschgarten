package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.Path
import com.intellij.util.containers.BidirectionalMap as IntellijBidirectionalMap

/**
 * Tests for utility functions in LanguagePluginUtil.
 */
class LanguagePluginUtilTest {
  private fun createTestBidirectionalMap(): BidirectionalMap<String, String> {
    val delegate = IntellijBidirectionalMap<String, String>()
    return object : BidirectionalMap<String, String>, MutableMap<String, String> by delegate {
      override fun getKeysByValue(value: String): List<String> = delegate.getKeysByValue(value) ?: emptyList()
    }
  }

  @Test
  fun `isInternalTarget returns true for main workspace targets`() {
    val label = Label.parse("//app:lib") as ResolvedLabel
    assertTrue(isInternalTarget(label, RepoMappingDisabled))
  }

  @Test
  fun `isInternalTarget returns true for Gazelle-generated targets`() {
    val label = Label.parse("@gazelle//pkg:target") as ResolvedLabel
    assertTrue(isInternalTarget(label, RepoMappingDisabled))
  }

  @Test
  fun `isInternalTarget returns true for bzlmod local repos`() {
    val label = Label.parse("@@rules_jvm_external~1.2.3~maven~com_google_guava_guava//jar:jar") as ResolvedLabel
    val repoMapping = BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = mapOf(
        "rules_jvm_external~1.2.3~maven~com_google_guava_guava" to Path("/workspace/external/guava")
      ),
      apparentRepoNameToCanonicalName = createTestBidirectionalMap(),
      canonicalRepoNameToPath = emptyMap(),
    )
    assertTrue(isInternalTarget(label, repoMapping))
  }

  @Test
  fun `isInternalTarget returns false for external targets without bzlmod mapping`() {
    val label = Label.parse("@external_repo//pkg:target") as ResolvedLabel
    assertFalse(isInternalTarget(label, RepoMappingDisabled))
  }

  @Test
  fun `hasSourcesWithExtensions returns true when extension matches`() {
    val target = BspTargetInfo.TargetInfo.newBuilder()
      .setId("//app:lib")
      .addSources(BspTargetInfo.FileLocation.newBuilder().setRelativePath("app/Main.java"))
      .addSources(BspTargetInfo.FileLocation.newBuilder().setRelativePath("app/Helper.kt"))
      .build()

    assertTrue(hasSourcesWithExtensions(target, ".java"))
    assertTrue(hasSourcesWithExtensions(target, ".kt"))
    assertTrue(hasSourcesWithExtensions(target, ".java", ".kt"))
  }

  @Test
  fun `hasSourcesWithExtensions returns false when extension does not match`() {
    val target = BspTargetInfo.TargetInfo.newBuilder()
      .setId("//app:lib")
      .addSources(BspTargetInfo.FileLocation.newBuilder().setRelativePath("app/Main.java"))
      .build()

    assertFalse(hasSourcesWithExtensions(target, ".py"))
    assertFalse(hasSourcesWithExtensions(target, ".go", ".rs"))
  }

  @Test
  fun `hasSourcesWithExtensions returns false for empty sources`() {
    val target = BspTargetInfo.TargetInfo.newBuilder()
      .setId("//app:lib")
      .build()

    assertFalse(hasSourcesWithExtensions(target, ".java"))
  }

  @Test
  fun `hasSourcesWithExtensions handles multiple file types`() {
    val target = BspTargetInfo.TargetInfo.newBuilder()
      .setId("//app:lib")
      .addSources(BspTargetInfo.FileLocation.newBuilder().setRelativePath("app/Main.java"))
      .addSources(BspTargetInfo.FileLocation.newBuilder().setRelativePath("app/Helper.kt"))
      .addSources(BspTargetInfo.FileLocation.newBuilder().setRelativePath("app/Config.xml"))
      .build()

    assertTrue(hasSourcesWithExtensions(target, ".java", ".kt", ".scala"))
    assertFalse(hasSourcesWithExtensions(target, ".py", ".go"))
  }
}
