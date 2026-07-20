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
// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.golang.resolve

import com.goide.psi.GoFile
import com.goide.psi.impl.GoPackage
import com.goide.sdk.GoPackageUtil
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.Processor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.references.resolveLabel
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntity
import org.jetbrains.bazel.workspacemodel.entities.targetKey
import java.util.function.Predicate

/**
 * Wrapper around [BazelGoPackageEntity] that implements [GoPackage] from the Go plugin.
 * Differences from vanilla [GoPackage]:
 * - Same directory may appear in different [BazelGoPackage]s.
 * - [files] will not necessarily return all files under [GoPackage.getDirectories].
 * - [navigate]s to the corresponding go rule instead of a directory.
 */
@ApiStatus.Internal
class BazelGoPackage(
  private val project: Project,
  private val entity: BazelGoPackageEntity,
) : GoPackage(
  project,
  getPackageName(project, entity.sources(), entity.importPath),
  *entity.sources().mapNotNull { it.parent }.distinct().toTypedArray(),
) {


  /**
   * Returns a list of resolve targets, one for each import path component.
   *
   * The final path component may either be the target name, or the containing directory name (in
   * which case the target name is usually `go_default_library`).
   *
   * For instance, consider the target `//foo:bar` with import path `"github.com/user/foo/bar"`:
   *
   * ```
   * import "github.com/user/foo/bar"
   *         (1)        (2)  (3) (4)
   * ```
   *
   * 1. `github.com` resolves to nothing, falls back to target `//foo:bar`.
   * 2. `user` resolves to nothing, falls back to target `//foo:bar`.
   * 3. `foo` resolves to the directory `foo/`.
   * 4. `bar` resolves to the target `//foo:bar`.
   *
   * Another example would be the target `//one/two:go_default_library` with import path `"github.com/user/one/two"`:
   *
   * ```
   * import "github.com/user/one/two"
   *         (1)        (2)  (3) (4)
   * ```
   *
   * 1. `github.com` resolves to nothing, falls back to directory `one/two/`.
   * 2. `user` resolves to nothing, falls back to directory `one/two/`.
   * 3. `one` resolves to the directory `one/`.
   * 4. `two` resolves to the directory `one/two/`.
   */
  fun getImportReferences(): Array<PsiElement>? {
    val navigable = getNavigableElement() ?: return null
    return getImportReferences(getMainLabel() ?: return null, navigable, entity.importPath)
  }

  companion object {
    /**
     * Resolves the import references for a given `Label`, `PsiElement`, and import path.
     *
     * @param label         the Bazel label associated with the import
     * @param buildElement  the PsiElement (build file or directory) for which references are resolved
     * @param importPath    the import path string to resolve
     * @return an array of resolved [PsiElement]s corresponding to the components of the import path
     */
    @VisibleForTesting
    fun getImportReferences(
      label: Label,
      buildElement: PsiElement,
      importPath: String,
    ): Array<PsiElement> {
      val pathComponents = importPath.split("/")
      val importReferences = pathComponents.indices.map { buildElement }.toTypedArray()

      if (pathComponents.isEmpty()) {
        return importReferences
      }

      // Get the last element (e.g., `bar` for the import path ending in `github.com/user/foo/bar`)
      val lastPathComponent = pathComponents.last()
      val lastElement = getLastElement(lastPathComponent, label, buildElement) ?: return importReferences

      importReferences[pathComponents.size - 1] = lastElement
      var currentElement: PsiDirectory?
      if (lastElement is PsiDirectory) {
        currentElement = lastElement.parent
      }
        else {
        val containingPackage = lastElement.containingFile?.parent
        if (containingPackage?.name == lastPathComponent) {
          // Package name is the same as last component -- go one level up again
          currentElement = containingPackage.parent
        } else {
          currentElement = containingPackage
        }
      }

      // Iterate backwards through the path components, linking names to the proper directories or elements
      for (i in pathComponents.size - 2 downTo 0) {
        if (currentElement == null) break

        val name = currentElement.name
        val pathComponent = pathComponents[i]
        if (name == pathComponent) {
          importReferences[i] = currentElement
          currentElement = currentElement.parent
        }
        else {
          break
        }
      }

      return importReferences
    }

    /**
     * Finds the last element of the import path.
     *
     * @param name          the name of the final component in the import path
     * @param label         the Bazel label
     * @param buildElement  the PsiElement representing the build file or directory
     * @return the corresponding resolved [PsiElement], or `null` if it cannot be found
     */
    private fun getLastElement(
      name: String,
      label: Label,
      buildElement: PsiElement,
    ): PsiElement? {
      return when (buildElement) {
        is StarlarkCallExpression -> {
          if (name == label.targetName) {
            buildElement
          }
          else {
            buildElement.containingFile?.parent?.takeIf {
              it.name == name
            }
          }
        }

        is StarlarkFile -> {
          if (!buildElement.isBuildFile()) return null
          buildElement.parent?.takeIf { it.name == name }
          ?: buildElement
        }

        else -> null
      }
    }

    /**
     * Package name is determined by package declaration in the source files.
     * By convention, this is *usually* equal to the last component of the `importpath`.
     * However, e.g., for `go_binary` the package is always `main`.
     *
     * `rules_go` in principle allows any package name, but enforces that among packages passed to the linker,
     * each `importpath` corresponds to exactly one package name.
     * Very conveniently (see doc [BazelGoPackageEntity]), [BazelGoPackage] contains files with only one `importpath`,
     * so checking the package name for just one source file, not for all sources, is enough here.
     */
    private fun getPackageName(
      project: Project,
      sources: List<VirtualFile>,
      importPath: String,
    ): String {
      val psiManager = PsiManager.getInstance(project)
      sources
        .mapNotNull { source ->
          source.let {
            psiManager.findFile(it) as? GoFile
          }
        }.firstOrNull { it.buildFlags != "ignore" }
        ?.let {
          return GoPackageUtil.getTrimmedPackageName(it) ?: importPath.substringAfterLast('/')
        }
      return importPath.substringAfterLast('/')
    }

    private fun BazelGoPackageEntity.sources(): List<VirtualFile> = sources.mapNotNull { it.virtualFile }
  }

  override fun files(): Collection<PsiFile> {
    val psiManager = PsiManager.getInstance(project)
    return entity.sources().mapNotNull { psiManager.findFile(it) }
  }

  override fun processFiles(processor: Processor<in PsiFile>, virtualFileFilter: Predicate<VirtualFile>): Boolean {
    if (!isValid) return true
    val fileIndexFacade = FileIndexFacade.getInstance(project)
    for (file in files()) {
      val virtualFile = file.virtualFile
      if (virtualFile.isValid &&
          virtualFileFilter.test(virtualFile) &&
          !fileIndexFacade.isExcludedFile(virtualFile) &&
          !processor.process(file)
      ) {
        return false
      }
    }
    return true
  }

  override fun navigate(requestFocus: Boolean) {
    val navigable = getNavigableElement()
    if (navigable is Navigatable && navigable.canNavigate()) {
      navigable.navigate(requestFocus)
    }
  }

  override fun canNavigate(): Boolean = getNavigableElement() is Navigatable

  /**
   * navigates to the target in the BUILD file or just the BUILD file
   */
  override fun getNavigableElement(): PsiElement? =
    resolveLabel(project, getMainLabel() ?: return null)

  /**
   * A package can actually correspond to several labels in case of `embed`.
   * Unfortunately, [getNavigableElement] allows for only one option, so we just take the first option.
   */
  private fun getMainLabel(): Label? {
    val targetUtils = project.targetUtils
    val labels = project.workspaceModel.currentSnapshot.referrers(entity.symbolicId, BazelGoTargetEntity::class.java)
      .map { it.targetKey.label }
    // A package may have a library target, and a test target that tests that library.
    // Don't resolve to a test target if we can.
    // TODO: migrate to full compound key
    labels.firstOrNull { targetUtils.getBuildTargetForLabel(it)?.kind?.ruleType == RuleType.LIBRARY }?.let { return it }
    return labels.firstOrNull()
  }

  override fun getCanonicalImport(contextModule: Module?): String = entity.importPath

  override fun getImportPath(withVendoring: Boolean): String = entity.importPath

  override fun getAllImportPaths(withVendoring: Boolean): List<String> = listOf(entity.importPath)

  override fun toString(): String = "Package: ${entity.importPath}"

  override fun equals(o: Any?): Boolean {
    if (this === o) return true
    if (javaClass != o?.javaClass) return false
    o as BazelGoPackage
    return entity == o.entity
  }

  override fun hashCode(): Int =
    entity.hashCode()
}
