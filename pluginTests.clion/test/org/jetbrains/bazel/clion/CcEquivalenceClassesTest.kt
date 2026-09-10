package org.jetbrains.bazel.clion

import com.intellij.testFramework.junit5.fixture.projectFixture
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.haveExactlyOne
import org.jetbrains.bazel.assertions.workspace
import org.jetbrains.bazel.clion.workspace.CcResolveConfiguration
import org.jetbrains.bazel.clion.workspace.buildCompilerSettings
import org.jetbrains.bazel.clion.workspace.buildEquivalenceClasses
import org.jetbrains.bazel.clion.workspace.buildToolchainMap
import org.jetbrains.bazel.fixtures.CcProjectBuilder
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.fixtures.withTestImportContext
import org.jetbrains.bazel.label.Label
import org.junit.jupiter.api.Test

private const val SAME_DEFINE = "SAME=1"
private const val DIFFERENT_DEFINE = "DIFFERENT=1"
private const val SAME_COPT = "-D$SAME_DEFINE"
private const val DIFFERENT_COPT = "-D$DIFFERENT_DEFINE"

@CcTestApplication
class CcEquivalenceClassesTest {

  private val project by projectFixture()

  @Test
  fun testGroupsEqualTargets() {
    val configurations = resolve {
      ccBinary { label("//foo/bar:one") }
      ccBinary { label("//foo/bar:two") }
      ccBinary { label("//foo/bar:three") }
    }

    assertThat(configurations).hasSize(1)

    val configuration = configurations.byName("@//foo/bar:one and 2 other target(s)")
    assertThat(configuration.copts).isEmpty()
    assertThat(configuration.conlyopts).isEmpty()
    assertThat(configuration.cxxopts).isEmpty()
    assertThat(configuration.transitiveDefines).isEmpty()
    assertThat(configuration.transitiveIncludes).haveExactlyOne(workspace("foo/bar"))
    assertThat(configuration.transitiveQuoteIncludes).isEmpty()
    assertThat(configuration.transitiveSystemIncludes).isEmpty()
  }

  @Test
  fun testCoptsSplitGroup() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        copts(SAME_COPT)
      }
      ccBinary {
        label("//foo/bar:two")
        copts(SAME_COPT)
      }
      ccBinary {
        label("//foo/bar:three")
        copts(DIFFERENT_COPT)
      }
    }

    assertThat(configurations).hasSize(2)
    assertThat(configurations.byName("@//foo/bar:one and 1 other target(s)").copts).containsExactly(SAME_COPT)
    assertThat(configurations.byName("@//foo/bar:three").copts).containsExactly(DIFFERENT_COPT)
  }

  @Test
  fun testIncludeSplitGroup() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        includes("foo/same")
      }
      ccBinary {
        label("//foo/bar:two")
        includes("foo/same")
      }
      ccBinary {
        label("//foo/bar:three")
        includes("foo/different")
      }
    }

    assertThat(configurations).hasSize(2)
    assertThat(configurations.byName("@//foo/bar:one and 1 other target(s)").transitiveIncludes).haveExactlyOne(workspace("foo/same"))
    assertThat(configurations.byName("@//foo/bar:three").transitiveIncludes).haveExactlyOne(workspace("foo/different"))
  }

  @Test
  fun testDefinesSplitGroup() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        defines(SAME_DEFINE)
      }
      ccBinary {
        label("//foo/bar:two")
        defines(SAME_DEFINE)
      }
      ccBinary {
        label("//foo/bar:three")
        defines(DIFFERENT_DEFINE)
      }
    }

    assertThat(configurations).hasSize(2)
    assertThat(configurations.byName("@//foo/bar:one and 1 other target(s)").transitiveDefines).containsExactly("SAME=1")
    assertThat(configurations.byName("@//foo/bar:three").transitiveDefines).containsExactly("DIFFERENT=1")
  }

  @Test
  fun testConlyoptsSplitGroup() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        conlyopts(SAME_COPT)
      }
      ccBinary {
        label("//foo/bar:two")
        conlyopts(DIFFERENT_COPT)
      }
      ccBinary {
        label("//foo/bar:three")
      }
    }

    assertThat(configurations).hasSize(3)
    assertThat(configurations.byName("@//foo/bar:one").conlyopts).containsExactly(SAME_COPT)
    assertThat(configurations.byName("@//foo/bar:two").conlyopts).containsExactly(DIFFERENT_COPT)
    assertThat(configurations.byName("@//foo/bar:three").conlyopts).isEmpty()
  }

  @Test
  fun testCxxoptsSplitGroup() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        cxxopts(SAME_COPT)
      }
      ccBinary {
        label("//foo/bar:two")
        cxxopts(DIFFERENT_COPT)
      }
      ccBinary {
        label("//foo/bar:three")
      }
    }

    assertThat(configurations).hasSize(3)
    assertThat(configurations.byName("@//foo/bar:one").cxxopts).containsExactly(SAME_COPT)
    assertThat(configurations.byName("@//foo/bar:two").cxxopts).containsExactly(DIFFERENT_COPT)
    assertThat(configurations.byName("@//foo/bar:three").cxxopts).isEmpty()
  }

  @Test
  fun testManyInOneGroup() {
    val configurations = resolve {
      for (i in 0 until 100) ccBinary {
        label("//foo/bar:$i")
        srcs("foo/bar/$i.c")
      }
    }

    assertThat(configurations).hasSize(1)
    assertThat(configurations.byName("@//foo/bar:0 and 99 other target(s)")).isNotNull()
  }

  @Test
  fun testConfigInGroupName() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        configurationId("configA")
      }
    }

    assertThat(configurations).hasSize(1)
    assertThat(configurations.byName("@//foo/bar:one (configA)")).isNotNull()
  }

  @Test
  fun testConfigSplitGroup() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        configurationId("configA")
      }
      ccBinary {
        label("//foo/bar:two")
        configurationId("configB")
      }
      ccBinary {
        label("//foo/bar:three")
      }
    }

    assertThat(configurations).hasSize(3)
    assertThat(configurations.byName("@//foo/bar:one (configA)").configuration.shortChecksum).isEqualTo("configA")
    assertThat(configurations.byName("@//foo/bar:two (configB)").configuration.shortChecksum).isEqualTo("configB")
    assertThat(configurations.byName("@//foo/bar:three").configuration.shortChecksum).isNull()
  }


  @Test
  fun testSameTargetWithDifferentConfigs() {
    val configurations = resolve {
      ccBinary {
        label("//foo/bar:one")
        configurationId("configA")
      }
      ccBinary {
        label("//foo/bar:one")
        configurationId("configB")
      }
    }

    assertThat(configurations).hasSize(2)
    assertThat(configurations.byName("@//foo/bar:one (configA)")).isNotNull()
    assertThat(configurations.byName("@//foo/bar:one (configB)")).isNotNull()
    assertThat(configurations.forLabel("//foo/bar:one")).hasSize(2)
  }

  @Test
  fun testCompilerSplitGroup() {
    val configurations = resolve {
      val gcc = ccToolchain {
        label("//toolchain:gcc")
        compilerName("gcc")
      }
      val clang = ccToolchain {
        label("//toolchain:clang")
        compilerName("clang")
      }

      ccBinary {
        label("//foo/bar:one")
        deps(gcc)
      }
      ccBinary {
        label("//foo/bar:two")
        deps(clang)
      }
    }

    assertThat(configurations).hasSize(2)
    assertThat(configurations.byName("@//foo/bar:one")).isNotNull()
    assertThat(configurations.byName("@//foo/bar:two")).isNotNull()
  }

  @Test
  fun testSameToolchains() {
    val configurations = resolve {
      val toolchainOne = ccToolchain { label("//toolchain:one") }
      val toolchainTwo = ccToolchain { label("//toolchain:two") }

      ccBinary {
        label("//foo/bar:one")
        deps(toolchainOne)
      }
      ccBinary {
        label("//foo/bar:two")
        deps(toolchainTwo)
      }
    }

    assertThat(configurations).hasSize(1)
    assertThat(configurations.byName("@//foo/bar:one and 1 other target(s)")).isNotNull()
  }

  @Test
  fun testPlainTargetHasNoGroup() {
    val configurations = resolve {
      ccBinary { label("//foo/bar:one") }
      plainTarget { label("//foo/bar:two") }
    }

    assertThat(configurations).hasSize(1)
    assertThat(configurations.forLabel("//foo/bar:one")).hasSize(1)
    assertThat(configurations.forLabel("//foo/bar:two")).hasSize(0)
  }

  @Test
  fun testDepsDoNotSplitGroup() {
    val configurations = resolve {
      val libOne = ccLibrary {
        label("//lib:one")
        copts(SAME_COPT)
      }
      val libTwo = ccLibrary {
        label("//lib:two")
        copts(DIFFERENT_COPT)
      }

      ccBinary {
        label("//foo/bar:one")
        deps(libOne)
      }
      ccBinary {
        label("//foo/bar:two")
        deps(libTwo)
      }
    }

    assertThat(configurations).hasSize(3)
    assertThat(configurations.byName("@//lib:one").copts).containsExactly(SAME_COPT)
    assertThat(configurations.byName("@//lib:two").copts).containsExactly(DIFFERENT_COPT)
    assertThat(configurations.byName("@//foo/bar:one and 1 other target(s)").copts).isEmpty()
  }

  private fun resolve(declare: CcProjectBuilder.() -> Unit): List<CcResolveConfiguration> {
    val (ctx, result) = withTestImportContext(ccProject(declare = declare), project = project) {
      val target2Toolchain = buildToolchainMap()
      val toolchain2Compiler = buildCompilerSettings()

      buildEquivalenceClasses(target2Toolchain.mapValues { toolchain2Compiler[it.value] })
    }
    assertThat(ctx.events).isEmpty()

    return result
  }
}

private fun List<CcResolveConfiguration>.names(): List<String> = map { it.name }

private fun List<CcResolveConfiguration>.byName(name: String): CcResolveConfiguration.EquivalenceClass {
  val matches = filter { it.name == name }
  assertThat(matches).describedAs("configuration '%s' among %s", name, names()).hasSize(1)

  return matches.single().shared
}

private fun List<CcResolveConfiguration>.forLabel(label: String): List<CcResolveConfiguration.EquivalenceClass> {
  return filter { it.targets.any { target -> target.label == Label.parse(label) } }.map { it.shared }
}
