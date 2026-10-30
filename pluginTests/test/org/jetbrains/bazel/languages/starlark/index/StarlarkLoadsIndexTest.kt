package org.jetbrains.bazel.languages.starlark.index

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkLoadsIndexTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun testSimpleLoadIndexesImportedNames() {
    myFixture.configureByText(
      StarlarkFileType,
      """load(":foo.bzl", "bar", "baz")""",
    )

    getIndexedFiles("bar").shouldBeSingleton()
    getIndexedFiles("baz").shouldBeSingleton()
  }

  @Test
  fun testNamedLoadIndexesOriginalName() {
    myFixture.configureByText(
      StarlarkFileType,
      """load(":foo.bzl", alias = "original_name")""",
    )

    getIndexedFiles("original_name").shouldBeSingleton()
    getIndexedFiles("alias").shouldBeEmpty()
  }

  @Test
  fun testMultipleLoadsFromDifferentFiles() {
    myFixture.addFileToProject(
      "a.bzl",
      """load(":common.bzl", "shared_func")""",
    )
    myFixture.addFileToProject(
      "b.bzl",
      """load(":common.bzl", "shared_func")""",
    )

    getIndexedFiles("shared_func").shouldHaveSize(2)
  }

  @Test
  fun testMixedNamedAndPositionalLoad() {
    myFixture.configureByText(
      StarlarkFileType,
      """load(":foo.bzl", "positional", alias = "named")""",
    )

    getIndexedFiles("positional").shouldBeSingleton()
    getIndexedFiles("named").shouldBeSingleton()
  }

  private fun getIndexedFiles(key: String): Collection<VirtualFile> =
    runReadActionBlocking {
      FileBasedIndex
        .getInstance()
        .getContainingFiles(StarlarkLoadsIndex.NAME, key, GlobalSearchScope.allScope(myFixture.project))
    }
}
