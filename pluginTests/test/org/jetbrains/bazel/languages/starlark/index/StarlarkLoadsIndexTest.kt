package org.jetbrains.bazel.languages.starlark.index

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.indexing.FileBasedIndex
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.bazel.languages.starlark.StarlarkFileType

class StarlarkLoadsIndexTest : BasePlatformTestCase() {

  fun testSimpleLoadIndexesImportedNames() {
    myFixture.configureByText(
      StarlarkFileType,
      """load(":foo.bzl", "bar", "baz")""",
    )

    val scope = GlobalSearchScope.allScope(project)
    getIndexedFiles("bar", scope).shouldBeSingleton()
    getIndexedFiles("baz", scope).shouldBeSingleton()
  }

  fun testNamedLoadIndexesOriginalName() {
    myFixture.configureByText(
      StarlarkFileType,
      """load(":foo.bzl", alias = "original_name")""",
    )

    val scope = GlobalSearchScope.allScope(project)
    getIndexedFiles("original_name", scope).shouldBeSingleton()
    getIndexedFiles("alias", scope).shouldBeEmpty()
  }

  fun testMultipleLoadsFromDifferentFiles() {
    myFixture.addFileToProject(
      "a.bzl",
      """load(":common.bzl", "shared_func")""",
    )
    myFixture.addFileToProject(
      "b.bzl",
      """load(":common.bzl", "shared_func")""",
    )

    val scope = GlobalSearchScope.allScope(project)
    getIndexedFiles("shared_func", scope).shouldHaveSize(2)
  }

  fun testMixedNamedAndPositionalLoad() {
    myFixture.configureByText(
      StarlarkFileType,
      """load(":foo.bzl", "positional", alias = "named")""",
    )

    val scope = GlobalSearchScope.allScope(project)
    getIndexedFiles("positional", scope).shouldBeSingleton()
    getIndexedFiles("named", scope).shouldBeSingleton()
  }

  private fun getIndexedFiles(key: String, scope: GlobalSearchScope) = FileBasedIndex
    .getInstance()
    .getContainingFiles(StarlarkLoadsIndex.NAME, key, scope)
}
