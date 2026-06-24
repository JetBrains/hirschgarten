package org.jetbrains.bazel.languages.starlark.bazel

import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.sync.environment.projectCtx
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

@TestApplication
class BazelGlobalFunctionsTest {

  companion object {
    private val projectFixture = projectFixture(openAfterCreation = true)
    private val codeInsightFixture by codeInsightFixture(projectFixture, tempPathFixture())
  }

  private fun getFunction(name: String): BazelGlobalFunction =
    BazelGlobalFunctions.getFunctionByName(name, codeInsightFixture.project)
      ?: error("Function '$name' not found")

  @TestFactory
  fun `builtin functions should be present across all versions`(): List<DynamicTest> {
    val builtinFunctionNames = listOf(
      "abs", "all", "any", "bool", "depset", "dict", "dir", "enumerate",
      "fail", "float", "getattr", "hasattr", "hash", "int", "len", "list",
      "max", "min", "print", "range", "repr", "reversed", "sorted", "str",
      "tuple", "type", "zip",
    )
    return builtinFunctionNames.flatMap { name ->
      majorVersions.map { version ->
        dynamicTest("$version: $name") {
          codeInsightFixture.project.projectCtx.bazelRelease = version
          BazelGlobalFunctions.getFunctionByName(name, codeInsightFixture.project) shouldNotBe null
        }
      }
    }
  }

  @TestFactory
  fun `builtin rules should be present across all versions`(): List<DynamicTest> {
    val ruleNames = listOf(
      "alias", "config_setting", "exports_files", "filegroup", "genrule",
      "glob", "java_binary", "java_library", "java_test", "load",
      "package", "package_group", "proto_library", "py_binary", "py_library",
      "py_test", "sh_binary", "sh_test", "test_suite",
    )
    return ruleNames.flatMap { name ->
      majorVersions.map { version ->
        dynamicTest("$version: $name") {
          codeInsightFixture.project.projectCtx.bazelRelease = version
          BazelGlobalFunctions.getFunctionByName(name, codeInsightFixture.project) shouldNotBe null
        }
      }
    }
  }

  @TestFactory
  fun `each version should provide its version-specific builtin functions`(): List<DynamicTest> =
    versionSpecificFunctions.flatMap { func ->
      buildList {
        addPresenceTest(func.name, func.introducedIn)
        if (func.backportedTo.isEmpty()) {
          addAbsenceTest(func.name, func.introducedIn)
        }
        func.backportedTo.forEach { backportVersion ->
          addPresenceTest(func.name, backportVersion)
          addAbsenceTest(func.name, backportVersion)
        }
      }
    }

  @TestFactory  
  fun `expected external kotlin functions should be present`(): List<DynamicTest> =
    expectedKotlinFunctions.map { name ->
      dynamicTest(name) {
        val fn = getFunction(name)
        fn.doc.shouldNotBeNull()
        fn.params.shouldNotBeEmpty()
      }
    }

  @TestFactory
  fun `expected external go functions should be present`(): List<DynamicTest> =
    expectedGoFunctions.map { name ->
      dynamicTest(name) {
        val fn = getFunction(name)
        fn.doc.shouldNotBeNull()
        fn.params.shouldNotBeEmpty()
      }
    }

  @TestFactory
  fun `rules should have common params`(): List<DynamicTest> =
    expectedRuleNames.map { name ->
      dynamicTest(name) {
        codeInsightFixture.project.projectCtx.bazelRelease = BazelRelease(9, 1)
        val paramNames = getFunction(name).params.map { it.name }
        paramNames.shouldContainAll(commonParamNames)
      }
    }

  @TestFactory
  fun `test rules should have test params`(): List<DynamicTest> =
    expectedRuleNames.filter { it.endsWith("_test") }.map { name ->
      dynamicTest(name) {
        codeInsightFixture.project.projectCtx.bazelRelease = BazelRelease(9, 1)
        val paramNames = getFunction(name).params.map { it.name }
        paramNames.shouldContainAll(testParamNames)
      }
    }

  @TestFactory
  fun `binary rules should have binary params`(): List<DynamicTest> =
    expectedRuleNames.filter { it.endsWith("_binary") }.map { name ->
      dynamicTest(name) {
        codeInsightFixture.project.projectCtx.bazelRelease = BazelRelease(9, 1)
        val paramNames = getFunction(name).params.map { it.name }
        paramNames.shouldContainAll(binaryParamNames)
      }
    }

  @TestFactory
  fun `docs should have backticks converted to code tags`(): List<DynamicTest> {
    val backtickConversionExamples = listOf(
      // builtins
      "select" to "<code>select()</code> is the helper function",
      "bool" to "returns <code>False</code> if the object is <code>None</code>",
      "hasattr" to "has an attribute or method of the given <code>name</code>",
      "hash" to "Java's <code>String.hashCode()</code>",
      "glob" to "<code>include</code>",
      "depset" to "Creates a depset.",
      // stardoc
      "kt_compiler_plugin" to "<code>plugins</code>",
    )
    return backtickConversionExamples.map { (name, expectedFragment) ->
      dynamicTest(name) {
        codeInsightFixture.project.projectCtx.bazelRelease = BazelRelease(9, 1)
        val func = getFunction(name)
        func.doc shouldNotBe null
        func.doc!! shouldContain expectedFragment
        func.doc!! shouldNotContain "`"
      }
    }
  }

  @TestFactory
  fun `docs should not contain non-HTTP links`(): List<DynamicTest> {
    val nonHttpLinkRegex = Regex("""<a\s[^>]*href\s*=\s*["'](?!https?://)[^"']*["'][^>]*>""", RegexOption.DOT_MATCHES_ALL)
    return BazelGlobalFunctions.globalFunctions(codeInsightFixture.project).flatMap { (name, func) ->
      majorVersions.map { version ->
        dynamicTest("$version - $name") {
          val allDocs = buildList {
            func.doc?.let(::add)
            func.params.mapNotNull { it.doc }.forEach(::add)
          }
          for (doc in allDocs) {
            nonHttpLinkRegex.find(doc)?.let {
              error("Non-HTTP <a> link found in '$name': ${it.value}")
            }
          }
        }
      }

    }
  }

  @TestFactory
  fun `docs should not contain bare markdown references`(): List<DynamicTest> {
    val bareMarkdownRegex = Regex("""\[([A-Za-z]\w*)](?!\()""")
    return BazelGlobalFunctions.globalFunctions(codeInsightFixture.project).flatMap { (name, func) ->
      majorVersions.map { version ->
        dynamicTest("$version - $name") {
          codeInsightFixture.project.projectCtx.bazelRelease = BazelRelease(9, 1)
          val allDocs = buildList {
            func.doc?.let(::add)
            func.params.mapNotNull { it.doc }.forEach(::add)
          }
          for (doc in allDocs) {
            bareMarkdownRegex.find(doc)?.let {
              error("Bare markdown reference found in '$name': ${it.value}")
            }
          }
        }
      }
    }
  }

  @TestFactory
  fun `docs should not contain markdown links`(): List<DynamicTest> {
    val markdownLinkRegex = Regex("""\[[^]]+]\([^)]+\)""")
    return BazelGlobalFunctions.globalFunctions(codeInsightFixture.project).flatMap { (name, func) ->
      majorVersions.map { version ->
        dynamicTest("$version - $name") {
          val allDocs = buildList {
            func.doc?.let(::add)
            func.params.mapNotNull { it.doc }.forEach(::add)
          }
          for (doc in allDocs) {
            markdownLinkRegex.find(doc)?.let {
              error("Markdown link found in '$name': ${it.value}")
            }
          }
        }
      }
    }
  }

  private fun MutableList<DynamicTest>.addPresenceTest(funcName: String, version: String) {
    add(dynamicTest("$version: $funcName is present") {
      codeInsightFixture.project.projectCtx.bazelRelease = version.toBazelRelease()
      BazelGlobalFunctions.getFunctionByName(funcName, codeInsightFixture.project)
        ?: error("Function '$funcName' not found for Bazel $version")
    })
  }

  private fun MutableList<DynamicTest>.addAbsenceTest(funcName: String, version: String) {
    val prevIndex = builtinVersions.indexOf(version) - 1
    if (prevIndex < 0) return
    val previousVersion = builtinVersions[prevIndex]
    val test = dynamicTest("$previousVersion: $funcName is absent") {
      codeInsightFixture.project.projectCtx.bazelRelease = previousVersion.toBazelRelease()
      BazelGlobalFunctions.getFunctionByName(funcName, codeInsightFixture.project)
        ?.let { error("Function '$funcName' should not exist for Bazel $previousVersion") }
    }
    add(test)
  }
}

private fun String.toBazelRelease(): BazelRelease {
  val (major, minor, patch) = split(".").map { it.toInt() }
  return BazelRelease(major, minor, patch)
}

private val majorVersions = listOf(BazelRelease(7, 5), BazelRelease(8), BazelRelease(9))

private val builtinVersions = listOf(
  "7.5.0", "7.6.0", "7.7.0", "8.0.0", "8.1.0", "8.2.0", "8.3.0",
  "8.4.0", "8.4.1", "8.5.0", "8.6.0", "9.0.0", "9.0.1", "9.1.0",
)

private val expectedKotlinFunctions = listOf(
  "define_kt_toolchain", "kotlin_repositories", "kt_compiler_plugin",
  "kt_javac_options", "kt_jvm_binary", "kt_jvm_import", "kt_jvm_library",
  "kt_jvm_test", "kt_kotlinc_options", "kt_ksp_plugin", "kt_plugin_cfg",
  "kt_register_toolchains", "ktlint_config", "ktlint_fix", "ktlint_test",
)

private val expectedGoFunctions = listOf(
  "go_binary", "go_cross_binary", "go_library", "go_path",
  "go_reset_target", "go_source", "go_test",
)

private val expectedBuiltinRules = listOf(
  "cc_binary", "cc_import", "cc_library", "cc_proto_library",
  "cc_shared_library", "cc_static_library", "cc_test", "cc_toolchain",
  "genrule",
  "java_binary", "java_import", "java_library", "java_lite_proto_library",
  "java_plugin", "java_proto_library", "java_runtime", "java_single_jar",
  "java_test", "java_toolchain",
  "objc_import", "objc_library",
  "proto_lang_toolchain", "proto_library", "proto_toolchain",
  "py_binary", "py_library", "py_proto_library", "py_runtime", "py_test",
  "sh_binary", "sh_library", "sh_test",
)

private val expectedRuleNames = expectedBuiltinRules + expectedKotlinFunctions + expectedGoFunctions

private val commonParamNames = listOf(
  "aspect_hints", "compatible_with", "deprecation", "exec_compatible_with",
  "exec_group_compatible_with", "exec_properties", "features", "package_metadata",
  "restricted_to", "tags", "target_compatible_with", "testonly", "toolchains", "visibility",
)

private val testParamNames = listOf(
  "args", "env", "env_inherit", "flaky", "local", "shard_count", "size", "timeout",
)

private val binaryParamNames = listOf(
  "args", "env", "output_licenses",
)

private data class VersionSpecificFunction(
  val name: String,
  val introducedIn: String,
  val backportedTo: List<String> = emptyList(),
)

private val versionSpecificFunctions = listOf(
  // Bazel 8.0
  VersionSpecificFunction(name = "exec_transition", introducedIn = "8.0.0"),
  VersionSpecificFunction(name = "ignore_directories", introducedIn = "8.0.0"),
  VersionSpecificFunction(name = "macro", introducedIn = "8.0.0"),
  VersionSpecificFunction(name = "proto_toolchain", introducedIn = "8.0.0"),
  VersionSpecificFunction(name = "py_proto_library", introducedIn = "8.0.0"),
  // Bazel 8.1
  VersionSpecificFunction(name = "set", introducedIn = "8.1.0"),
  // Bazel 9.0
  VersionSpecificFunction(name = "MaterializedDepsInfo", introducedIn = "9.0.0"),
  VersionSpecificFunction(name = "java_single_jar", introducedIn = "9.0.0"),
  VersionSpecificFunction(name = "materializer_rule", introducedIn = "9.0.0"),
  VersionSpecificFunction(name = "package_default_visibility", introducedIn = "9.0.0"),
  VersionSpecificFunction(name = "flag_alias", introducedIn = "9.0.0", backportedTo = listOf("7.7.0", "8.5.0")),
)
