package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class GeneratedSourcesTest(bazelVersion: String) {

  @CcTestApplication
  class Bazel7 : GeneratedSourcesTest(BazelVersions.BAZEL_7)

  @CcTestApplication
  class Bazel8 : GeneratedSourcesTest(BazelVersions.BAZEL_8)

  @CcTestApplication
  class Bazel9 : GeneratedSourcesTest(BazelVersions.BAZEL_9)

  private val project by clionBazelProjectFixture("clion/generated_sources", bazelVersion = bazelVersion, buildProject = true)

  @Test
  fun testVfsRoots() = project.assertVfsLoads()

  @Test
  fun testNoEmptyResolveConfigurations(): Unit = timeoutRunBlocking {
    val configurations = OCWorkspace.getInstance(project).configurations
    assertThat(configurations.filter { it.sources.isEmpty() }).isEmpty()
  }

  @Test
  fun testSharedLibraryNotInResolveConfigurations(): Unit = timeoutRunBlocking {
    val configurations = OCWorkspace.getInstance(project).configurations
    assertThat(configurations.filter { it.sources.any { it.name.contains("libshared") } }).isEmpty()
  }
}
