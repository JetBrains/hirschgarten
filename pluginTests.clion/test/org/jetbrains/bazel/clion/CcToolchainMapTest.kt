package org.jetbrains.bazel.clion

import com.intellij.build.events.MessageEvent
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.clion.workspace.buildToolchainMap
import org.jetbrains.bazel.fixtures.CcProjectBuilder
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.TestImportEvent
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.fixtures.withTestImportContext
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.junit.jupiter.api.Test

@CcTestApplication
class CcToolchainMapTest {

  private val project by projectFixture()

  @Test
  fun testCompileDep() {
    val (events, toolchains) = resolve {
      val toolchain = ccToolchain { label("//toolchain:only") }

      ccBinary {
        label("//main:main")
        deps(DependencyLabelKind.COMPILE, toolchain)
      }
    }

    assertThat(events).isEmpty()
    assertThat(toolchains.byLabel("//main:main")).hasLabel("//toolchain:only")
  }

  @Test
  fun testToolchainDep() {
    val (events, toolchains) = resolve {
      val toolchain = ccToolchain { label("//toolchain:only") }

      ccBinary {
        label("//main:main")
        deps(DependencyLabelKind.TOOLCHAIN, toolchain)
      }
    }

    assertThat(events).isEmpty()
    assertThat(toolchains.byLabel("//main:main")).hasLabel("//toolchain:only")
  }


  @Test
  fun testToolchainDepOverwrites() {
    val (events, toolchains) = resolve {
      val toolchain = ccToolchain { label("//toolchain:only") }
      val compile = ccToolchain { label("//toolchain:compile") }

      ccBinary {
        label("//main:main")
        deps(DependencyLabelKind.COMPILE, compile)
        deps(DependencyLabelKind.TOOLCHAIN, toolchain)
      }
    }

    assertThat(events).isEmpty()
    assertThat(toolchains.byLabel("//main:main")).hasLabel("//toolchain:only")
  }

  @Test
  fun testDifferentToolchains() {
    val (events, toolchains) = resolve {
      val one = ccToolchain { label("//toolchain:one") }
      val two = ccToolchain { label("//toolchain:two") }

      ccBinary {
        label("//main:main")
        deps(DependencyLabelKind.TOOLCHAIN, one)
      }
      ccLibrary {
        label("//lib:lib")
        deps(DependencyLabelKind.TOOLCHAIN, two)
      }
    }

    assertThat(events).isEmpty()
    assertThat(toolchains.byLabel("//main:main")).hasLabel("//toolchain:one")
    assertThat(toolchains.byLabel("//lib:lib")).hasLabel("//toolchain:two")
  }

  @Test
  fun testFallbackToolchain() {
    val (events, toolchains) = resolve {
      ccToolchain { label("//toolchain:fallback") }

      ccBinary {
        label("//main:main")
        noToolchain()
      }
    }

    assertThat(events).hasSize(1)
    assertThat(toolchains.byLabel("//main:main")).hasLabel("//toolchain:fallback")

    val issue = events.single()
    assertThat(issue.severity).isEqualTo(MessageEvent.Kind.WARNING)
    assertThat(issue.message).isEqualTo("Unexpected number of C/C++ toolchain dependencies for 1 target(s)")
    assertThat(issue.description).isEqualTo("@//main:main: No dependency on a C/C++ toolchain found")
  }

  @Test
  fun testMultipleToolchains() {
    val (events, toolchains) = resolve {
      val one = ccToolchain { label("//toolchain:one") }
      val two = ccToolchain { label("//toolchain:two") }

      ccBinary {
        label("//main:main")
        deps(DependencyLabelKind.TOOLCHAIN, one, two)
      }
    }

    assertThat(events).hasSize(1)
    assertThat(toolchains.byLabel("//main:main")).hasLabel("//toolchain:one")

    val issue = events.single()
    assertThat(issue.severity).isEqualTo(MessageEvent.Kind.WARNING)
    assertThat(issue.message).isEqualTo("Unexpected number of C/C++ toolchain dependencies for 1 target(s)")
    assertThat(issue.description).isEqualTo("@//main:main: @//toolchain:one, @//toolchain:two")
  }

  @Test
  fun testNoToolchain() {
    val (events, toolchains) = resolve {
      ccBinary {
        label("//main:main")
        noToolchain()
      }
    }

    assertThat(events).hasSize(1)
    assertThat(toolchains).isEmpty()

    val issue = events.single()
    assertThat(issue.severity).isEqualTo(MessageEvent.Kind.WARNING)
    assertThat(issue.message).isEqualTo("Unexpected number of C/C++ toolchain dependencies for 1 target(s)")
    assertThat(issue.description).contains("@//main:main: No dependency on a C/C++ toolchain found")
  }

  @Test
  fun testInvalidToolchainDependency() {
    val (events, toolchains) = resolve {
      ccToolchain { label("//toolchain:fallback") }

      val library = ccLibrary { label("//lib:lib") }

      ccBinary {
        label("//main:main")
        noToolchain()
        deps(DependencyLabelKind.TOOLCHAIN, library)
        deps(DependencyLabelKind.COMPILE, library)
      }
    }

    assertThat(events).hasSize(1)
    assertThat(toolchains.byLabel("//main:main")).hasLabel("//toolchain:fallback")
  }

  @Test
  fun testNonCcTarget() {
    val (events, toolchains) = resolve {
      ccToolchain { label("//toolchain:only") }
      plainTarget { label("//main:resources") }
    }

    assertThat(events).isEmpty()
    assertThat(toolchains).isEmpty()
  }

  private fun resolve(
    declare: CcProjectBuilder.() -> Unit,
  ): Pair<List<TestImportEvent>, Map<WorkspaceTargetKey, WorkspaceTargetKey>> = timeoutRunBlocking {
    val (ctx, result) = withTestImportContext(ccProject(declare = declare), project) { buildToolchainMap() }
    ctx.events to result
  }
}

private fun Map<WorkspaceTargetKey, WorkspaceTargetKey>.byLabel(label: String): WorkspaceTargetKey? {
  return entries.firstOrNull { it.key.label == Label.parse(label) }?.value
}
