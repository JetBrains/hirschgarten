/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.golang.resolve

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.test.framework.annotation.BazelTest
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** Tests for [BazelGoPackage.getImportReferences]. */
@BazelTest
class BazelGoPackageImportReferencesTest : MockProjectBaseTest() {
  @Test
  fun testFromBuildFile() {
    val label = Label.parse("//foo/bar:baz")
    val workspaceDirectory = mockPsiDirectory("workspace", null)
    val fooDirectory = mockPsiDirectory("foo", workspaceDirectory)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBuild = mockBuildFile(fooBarDirectory)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBuild, "foo/bar/baz")
    importReferences shouldBe
      arrayOf(
        fooDirectory, // foo
        fooBarDirectory, // bar
        fooBarBuild, // baz
      )
  }

  @Test
  fun testFromRule() {
    val label = Label.parse("//foo/bar:baz")
    val workspaceDirectory = mockPsiDirectory("workspace", null)
    val fooDirectory = mockPsiDirectory("foo", workspaceDirectory)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBuild = mockBuildFile(fooBarDirectory)
    val fooBarBazRule = mockRule("baz", fooBarBuild)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBazRule, "foo/bar/baz")
    importReferences shouldBe
      arrayOf(
        fooDirectory, // foo
        fooBarDirectory, // bar
        fooBarBazRule, // baz
      )
  }

  @Test
  fun testFromDefaultLibraryBuildFile() {
    val label = Label.parse("//foo/bar/baz:go_default_library")
    val workspaceDirectory = mockPsiDirectory("workspace", null)
    val fooDirectory = mockPsiDirectory("foo", workspaceDirectory)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBazDirectory = mockPsiDirectory("baz", fooBarDirectory)
    val fooBarBazBuild = mockBuildFile(fooBarBazDirectory)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBazBuild, "foo/bar/baz")
    importReferences shouldBe
      arrayOf(
        fooDirectory, // foo
        fooBarDirectory, // bar
        fooBarBazDirectory, // baz
      )
  }

  @Test
  fun testFromDefaultLibraryRule() {
    val label = Label.parse("//foo/bar/baz:go_default_library")
    val workspaceDirectory = mockPsiDirectory("workspace", null)
    val fooDirectory = mockPsiDirectory("foo", workspaceDirectory)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBazDirectory = mockPsiDirectory("baz", fooBarDirectory)
    val fooBarBazBuild = mockBuildFile(fooBarBazDirectory)
    val fooBarBazGoDefaultLibraryRule = mockRule("go_default_library", fooBarBazBuild)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBazGoDefaultLibraryRule, "foo/bar/baz")
    importReferences shouldBe
      arrayOf(
        fooDirectory, // foo
        fooBarDirectory, // bar
        fooBarBazDirectory, // baz
      )
  }

  @Test
  fun testResolvableImportPrefix() {
    val label = Label.parse("//foo/bar:baz")
    val externalGithubDirectory = mockPsiDirectory("github.com", null)
    val externalGithubUserDirectory = mockPsiDirectory("user", externalGithubDirectory)
    val fooDirectory = mockPsiDirectory("foo", externalGithubUserDirectory)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBuild = mockBuildFile(fooBarDirectory)
    val fooBarBazRule = mockRule("baz", fooBarBuild)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBazRule, "github.com/user/foo/bar/baz")
    importReferences shouldBe
      arrayOf(
        externalGithubDirectory, // github.com
        externalGithubUserDirectory, // user
        fooDirectory, // foo
        fooBarDirectory, // bar
        fooBarBazRule, // baz
      )
  }

  @Test
  fun testUnresolvableImportPrefix() {
    val label = Label.parse("//foo/bar:baz")
    val externalHomeDirectory = mockPsiDirectory("home", null)
    val externalHomeProjectDirectory = mockPsiDirectory("project", externalHomeDirectory)
    val fooDirectory = mockPsiDirectory("foo", externalHomeProjectDirectory)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBuild = mockBuildFile(fooBarDirectory)
    val fooBarBazRule = mockRule("baz", fooBarBuild)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBazRule, "github.com/user/foo/bar/baz")
    importReferences shouldBe
      arrayOf(
        null, // github.com
        null, // user
        fooDirectory, // foo
        fooBarDirectory, // bar
        fooBarBazRule, // baz
      )
  }

  @Test
  fun testStopsAtRootDirectory() {
    val label = Label.parse("//foo/bar:baz")
    val fooDirectory = mockPsiDirectory("foo", null)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBuild = mockBuildFile(fooBarDirectory)
    val fooBarBazRule = mockRule("baz", fooBarBuild)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBazRule, "github.com/user/foo/bar/baz")
    importReferences shouldBe
      arrayOf(
        null, // github.com
        null, // user
        fooDirectory, // foo
        fooBarDirectory, // bar
        fooBarBazRule, // baz
      )
  }

  @Test
  fun testUnrelatedImportPath() {
    val label = Label.parse("//foo/bar:baz")
    val externalHomeDirectory = mockPsiDirectory("home", null)
    val externalHomeProjectDirectory = mockPsiDirectory("project", externalHomeDirectory)
    val fooDirectory = mockPsiDirectory("foo", externalHomeProjectDirectory)
    val fooBarDirectory = mockPsiDirectory("bar", fooDirectory)
    val fooBarBuild = mockBuildFile(fooBarDirectory)
    val fooBarBazRule = mockRule("baz", fooBarBuild)
    val importReferences = BazelGoPackage.getImportReferences(label, fooBarBazRule, "one/two/three")
    importReferences shouldBe
      arrayOf<PsiElement?>(
        null, // one
        null, // two
        null, // three
      )
  }

  private fun mockPsiDirectory(name: String, parent: PsiDirectory?): PsiDirectory {
    val directory = mock(PsiDirectory::class.java)
    `when`(directory.name).thenReturn(name)
    `when`(directory.parent).thenReturn(parent)
    `when`(directory.parentDirectory).thenReturn(parent)
    return directory
  }

  private fun mockBuildFile(parent: PsiDirectory): StarlarkFile {
    val buildFile = mock(StarlarkFile::class.java)
    `when`(buildFile.name).thenReturn("BUILD")
    `when`(buildFile.isBuildFile()).thenReturn(true)
    `when`(buildFile.parent).thenReturn(parent)
    `when`(buildFile.containingFile).thenReturn(buildFile)
    return buildFile
  }

  private fun mockRule(name: String, buildFile: StarlarkFile): StarlarkCallExpression {
    val rule = mock(StarlarkCallExpression::class.java)
    `when`(rule.name).thenReturn(name)
    `when`(rule.parent).thenReturn(buildFile)
    `when`(rule.containingFile).thenReturn(buildFile)
    return rule
  }
}
