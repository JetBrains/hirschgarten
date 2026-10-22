package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.languages.starlark.references.getCanonicalRepoNameToBzlFiles
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.externalRepositoryPath
import org.jetbrains.bazel.test.framework.testproject.projectDirectories
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelLoadFilepathCompletionTest {
  private val myFixture by bazelCodeInsightFixture()
  private val testBazel get() = myFixture.testBazel

  @BeforeEach
  fun setUp() {
    testBazel.projectDirectories()
  }

  @Test
  fun `should complete basic filePath`() {
    testBazel.writeFile("BUILD.bazel")
    testBazel.writeFile("defs_one.bzl")
    testBazel.writeFile("defs_dir/defs_two.bzl")

    getCanonicalRepoNameToBzlFiles(myFixture.project)

    myFixture.configureByText("BUILD.bazel", """load("//:<caret>)""")
    myFixture.type("d")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainAll listOf("\"//:defs_one.bzl\"", "\"//:defs_dir/defs_two.bzl\"")
  }

  @Test
  fun `should complete external filePath`() {
    testBazel.writeFile("BUILD.bazel")
    testBazel.sync { repositories { external("rules_kotlin") } }
    val repository = testBazel.bazelInfo.externalRepositoryPath("rules_kotlin")
    for (relativePath in listOf("MODULE.bazel", "BUILD.bazel", "kt_jvm_library.bzl", "package/kt_jvm_binary.bzl")) {
      testBazel.writeFile(repository.resolve(relativePath))
    }
    getCanonicalRepoNameToBzlFiles(myFixture.project)

    myFixture.configureByText("BUILD.bazel", """load("//:<caret>)""")
    myFixture.type("kt")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainAll
      listOf(
        "\"@rules_kotlin//:kt_jvm_library.bzl\"",
        "\"@rules_kotlin//:package/kt_jvm_binary.bzl\"",
      )
  }

  @Test
  fun `should complete for use_extension`() {
    testBazel.sync { repositories { external("rules_jvm_external") } }
    val repository = testBazel.bazelInfo.externalRepositoryPath("rules_jvm_external")
    for (relativePath in listOf("MODULE.bazel", "BUILD.bazel", "defs.bzl", "extensions.bzl")) {
      testBazel.writeFile(repository.resolve(relativePath))
    }
    getCanonicalRepoNameToBzlFiles(myFixture.project)

    myFixture.configureByText("MODULE.bazel", """maven = use_extension("<caret>)""")
    myFixture.type("@ru")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainAll
      listOf(
        "\"@rules_jvm_external//:extensions.bzl\"",
        "\"@rules_jvm_external//:defs.bzl\"",
      )
  }
}
