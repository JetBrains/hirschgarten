package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo
import org.jetbrains.bazel.clion.workspace.buildCompilerSettings
import org.jetbrains.bazel.fixtures.CcProjectBuilder
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.fixtures.withTestImportContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@DisabledOnOs(OS.WINDOWS, disabledReason = "Bazel emits /proc/self/cwd only from a Linux cc_toolchain")
class CcCompilerSettingsTest {

  private val project by projectFixture()

  @Test
  fun testRewritesProcSelfCwdValueToAbsolutePath() {
    val compiler = resolve {
      ccToolchain { env("QNX_HOST" to "/proc/self/cwd/external/qnx+/host") }
    }

    assertThat(compiler.environment).containsEntry("QNX_HOST", "/execroot/external/qnx+/host")
  }

  @Test
  fun testRewritesProcSelfCwdOutputValueToAbsolutePath() {
    val compiler = resolve {
      ccToolchain { env("WRAPPER" to "/proc/self/cwd/bazel-out/k8-opt/bin/tools/wrapper") }
    }

    assertThat(compiler.environment).containsEntry("WRAPPER", "/execroot/bazel-out/k8-opt/bin/tools/wrapper")
  }

  @Test
  fun testLeavesAValueWithoutTheMarkerUnchanged() {
    val compiler = resolve {
      ccToolchain {
        env(
          "PLAIN" to "bar",
          "RELATIVE" to "some/relative/value",
          "NUMBER" to "2",
          "PATH" to "/usr/bin:/bin",
        )
      }
    }

    assertThat(compiler.environment)
      .containsEntry("PLAIN", "bar")
      .containsEntry("RELATIVE", "some/relative/value")
      .containsEntry("NUMBER", "2")
      .containsEntry("PATH", "/usr/bin:/bin")
  }

  @Test
  fun testKeepsAValueThatIsNotAPathOnThisPlatform() {
    val compiler = resolve {
      ccToolchain { env("BROKEN" to "/proc/self/cwd/foo\u0000bar") }
    }

    assertThat(compiler.environment).containsEntry("BROKEN", "/proc/self/cwd/foo\u0000bar")
  }

  @Test
  fun testLeavesAnEmbeddedMarkerUnchanged() {
    // the marker counts only as a prefix. A `contains` check would rewrite these to a bogus path
    val compiler = resolve {
      ccToolchain {
        env(
          "LDFLAGS" to "-L/proc/self/cwd/lib",
          "SEARCH" to "/usr/bin:/proc/self/cwd/bin",
        )
      }
    }

    assertThat(compiler.environment)
      .containsEntry("LDFLAGS", "-L/proc/self/cwd/lib")
      .containsEntry("SEARCH", "/usr/bin:/proc/self/cwd/bin")
  }

  private fun resolve(declare: CcProjectBuilder.() -> Unit): CcCompilerInfo = timeoutRunBlocking {
    val (ctx, compilers) = withTestImportContext(ccProject(declare = declare), project = project) {
      buildCompilerSettings()
    }

    assertThat(ctx.events).isEmpty()
    assertThat(compilers).hasSize(1)

    compilers.values.single()
  }
}
