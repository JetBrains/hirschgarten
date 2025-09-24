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

import com.goide.psi.GoFile
import com.goide.psi.impl.GoPackage
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSet.toImmutableSet
import com.google.common.collect.Streams
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.Processor
import one.util.streamex.StreamEx
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.golang.targetKinds.GoBazelRules
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.references.findBuildFile
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.sync.hasLanguage
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.testing.TestUtils
import org.jetbrains.bazel.utils.findVirtualFile
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Objects
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import kotlin.io.path.invariantSeparatorsPathString

/**
 * [GoPackage] specialized for bazel, with a couple of differences:
 *
 *   - Same directory may appear in may different [BazelGoPackage]s.
 *   - A single [BazelGoPackage] may contain may different directories.
 *   - [files] will not necessarily return all files under [GoPackage#getDirectories()].
 *   - [#navigate]s to the corresponding go rule instead of a directory.
 *
 * Exactly one [BazelGoPackage] per go rule.
 */
class BazelGoPackage : GoPackage {
  private val label: Label
  private val importPath: String
  private val files: MutableMap<Path, Optional<PsiFile>>
  private val directories: MutableMap<Path, Optional<VirtualFile>>

  @Volatile
  private var navigableElement: PsiElement? = null

  @Volatile
  private var importReferences: Array<PsiElement?>? = null

  constructor(
    project: Project,
    importPath: String,
    target: BuildTarget,
  ) : this(
    project = project,
    importPath = importPath,
    label = target.id,
    files = getTargetToFileMap(project)[target.id].toList(),
  )

  private constructor(
    project: Project,
    importPath: String,
    label: Label,
    files: List<Path>,
  ) : super(project, getPackageName(project, files, importPath)) {
    this.importPath = importPath
    this.label = label
    this.files =
      ConcurrentHashMap<Path, Optional<PsiFile>>().apply {
        files.forEach { put(it, Optional.empty()) }
      }
    this.directories =
      ConcurrentHashMap<Path, Optional<VirtualFile>>().apply {
        files.mapNotNull { it.parent }.forEach { put(it, Optional.empty()) }
      }
  }

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
   * 1. `github.com` resolves to nothing.
   * 2. `user` resolves to nothing.
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
   * 1. `github.com` resolves to nothing.
   * 2. `user` resolves to nothing.
   * 3. `one` resolves to the directory `one/`.
   * 4. `two` resolves to the directory `one/two/`.
   */
  fun getImportReferences(): Array<PsiElement?>? {
    val references = importReferences
    if (references != null && references.all { it == null || it.isValid }) {
      return references
    }

    val navigable = getNavigableElement() ?: return null

    importReferences = getImportReferences(label, navigable, importPath)
    return importReferences
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
    ): Array<PsiElement?> {
      val pathComponents = importPath.split("/")
      val importReferences = arrayOfNulls<PsiElement>(pathComponents.size)

      if (pathComponents.isEmpty()) {
        return importReferences
      }

      // Get the last element (e.g., `bar` for the import path ending in `github.com/user/foo/bar`)
      val lastElement = getLastElement(pathComponents.last(), label, buildElement) ?: return importReferences

      importReferences[pathComponents.size - 1] = lastElement
      var currentElement =
        if (lastElement is PsiDirectory) {
          lastElement.parent
        } else {
          lastElement.containingFile?.parent
        }

      // Iterate backwards through the path components, linking names to the proper directories or elements
      for (i in pathComponents.size - 2 downTo 0) {
        if (currentElement == null) break

        val name = currentElement.name
        val pathComponent = pathComponents[i]
        if (name == pathComponent) {
          importReferences[i] = currentElement
          currentElement = currentElement.parent
        } else {
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
          } else {
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

    private val goTargetToFileMap =
      SyncCache.SyncCacheComputable { project ->
        getUncachedTargetToFileMap(project)
      }

    fun getTargetToFileMap(project: Project): ImmutableMultimap<Label, Path> =
      SyncCache
        .getInstance(project)
        .get(goTargetToFileMap)

    fun getUncachedTargetToFileMap(project: Project): ImmutableMultimap<Label, Path> {
      val libraryToTestMap = buildLibraryToTestMap(project)
      val builder = ImmutableMultimap.builder<Label, Path>()
      project.targetUtils.allBuildTargets().filter { extractGoBuildTarget(it) != null }.forEach { target ->
        val sourceFiles = getSourceFiles(target, libraryToTestMap).map { toRealFile(it) }
        builder.putAll(target.id, sourceFiles)
      }
      return builder.build()
    }

    private fun buildLibraryToTestMap(project: Project): ImmutableMultimap<Label, GoBuildTarget> {
      val targetUtils = project.targetUtils
      val builder = ImmutableMultimap.builder<Label, GoBuildTarget>()
      targetUtils.allBuildTargets().forEach { target ->
        if (target.kind.hasLanguage(LanguageClass.GO) &&
          target.kind.ruleType == RuleType.TEST
        ) {
          val goBuildTarget = extractGoBuildTarget(target) ?: return@forEach
          goBuildTarget.libraryLabels.forEach { libraryLabel ->
            builder.put(libraryLabel, goBuildTarget)
          }
        }
      }
      return builder.build()
    }

    private fun getSourceFiles(target: BuildTarget, libraryToTestMap: ImmutableMultimap<Label, GoBuildTarget>): ImmutableSet<Path> {
      if (target.kind == GoBazelRules.RuleTypes.GO_WRAP_CC.kind) {
        return getWrapCcGoFiles(target)
      }
      val sources = mutableSetOf<Path>()
      val targetSources = extractGoBuildTarget(target)?.generatedSources
      val librarySources = libraryToTestMap[target.id].flatMap { it.generatedSources }

      (targetSources.orEmpty() + librarySources).toCollection(sources)

      return ImmutableSet.copyOf(sources)
    }

    private fun getWrapCcGoFiles(target: BuildTarget): ImmutableSet<Path> {
      val goBuildTarget = extractGoBuildTarget(target) ?: return ImmutableSet.of()
      return goBuildTarget.generatedSources.stream().collect(toImmutableSet())
    }

    private fun getPackageName(
      project: Project,
      files: List<Path>,
      importPath: String,
    ): String {
      val psiManager = PsiManager.getInstance(project)
      files
        .mapNotNull { file ->
          file.findVirtualFile()?.let {
            psiManager.findFile(it) as? GoFile
          }
        }.firstOrNull { it.buildFlags != "ignore" }
        ?.let {
          return it.canonicalPackageName ?: importPath.substringAfterLast('/')
        }
      return importPath.substringAfterLast('/')
    }
  }

  override fun getDirectories(): MutableSet<VirtualFile> {
    directories.replaceAll { file, oldVirtualFile ->
      if (oldVirtualFile.filter(VirtualFile::isValid).isPresent) {
        oldVirtualFile
      } else {
        Optional.ofNullable(file.findVirtualFile())
      }
    }
    return directories.values
      .stream()
      .flatMap(Streams::stream)
      .filter(VirtualFile::isValid)
      .collect(toImmutableSet())
  }

  override fun files(): MutableCollection<PsiFile> {
    val psiManager = PsiManager.getInstance(project)
    files.replaceAll { file, oldGoFile ->
      if (oldGoFile.filter(PsiFile::isValid).isPresent) {
        oldGoFile
      } else {
        Optional
          .ofNullable(file.findVirtualFile())
          .map(psiManager::findFile)
          .filter { it is GoFile }
          .map { it as GoFile }
      }
    }
    return files.values
      .stream()
      .flatMap(Streams::stream)
      .filter(PsiFile::isValid)
      .collect(toImmutableSet())
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
  override fun getNavigableElement(): PsiElement? {
    navigableElement?.takeIf { it.isValid }?.let { return it }
    if (label is ResolvedLabel) {
      val buildFile = findBuildFile(project, label, null)
      buildFile?.also { navigableElement = it }?.findRuleTarget(label.targetName)?.also { navigableElement = it }
    }

    return navigableElement
  }

  override fun getCanonicalImport(contextModule: Module?): String = importPath

  override fun getImportPath(withVendoring: Boolean): String = importPath

  override fun getAllImportPaths(withVendoring: Boolean): List<String> = listOf(importPath)

  override fun toString(): String = "Package: $importPath"

  /**
   * need to override these because [GoPackage] uses [GoPackage#myDirectories] directly in their implementations
   */
  override fun isValid(): Boolean = getDirectories().size == directories.size

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BazelGoPackage) return false
    return importPath == other.importPath && directories == other.directories
  }

  override fun hashCode(): Int = Objects.hash(importPath, directories)

  override fun getPsiDirectories(): StreamEx<PsiDirectory> {
    val psiManager = PsiManager.getInstance(project)
    return StreamEx.of(getDirectories().mapNotNull { directory -> psiManager.findDirectory(directory)?.takeIf { it.isValid } })
  }
}

/**
 * Workaround for https://github.com/bazelbuild/intellij/issues/2057. External workspace symlinks
 * can be changed externally by practically any bazel command. Such changes to symlinks will make
 * IntelliJ red. This helper resolves such symlink to an actual location.
 *
 * In IDE Starter Test, this logic does not work properly, so it is disabled.
 */
@VisibleForTesting
fun toRealFile(maybeExternal: Path): Path {
  if (TestUtils.isInIdeStarterTest()) return maybeExternal
  val externalString = maybeExternal.invariantSeparatorsPathString
  return if (externalString.contains("/external/") &&
    !externalString.contains("/bazel-out/") &&
    !externalString.contains("/blaze-out/")
  ) {
    Paths.get(externalString.replace(Regex("/execroot.*?/external/"), "/external/"))
  } else {
    maybeExternal
  }
}
