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
package org.jetbrains.plugins.bsp.golang.resolve

import BspVfsUtils
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.goide.psi.GoFile
import com.goide.psi.impl.GoPackage
import com.google.common.collect.ImmutableSet.toImmutableSet
import com.google.common.collect.Streams
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.Processor
import com.intellij.util.containers.MultiMap
import one.util.streamex.StreamEx
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.bsp.protocol.utils.extractProtoBuildTarget
import org.jetbrains.plugins.bsp.impl.projectAware.BspSyncCache
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import java.io.File
import java.util.Objects
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Predicate
import kotlin.io.path.toPath

private const val GO_TARGET_TO_FILE_MAP_KEY = "BlazeGoPackage.targetToFileMap"

/**
 * [GoPackage] specialized for Bazel, with a couple of differences:
 *
 *
 *  1. Same directory may appear in many different [BlazeGoPackage]s.
 *  1. A single [BlazeGoPackage] may contain may different directories.
 *  1. [.files] will not necessarily return all files under [       ][GoPackage.getDirectories].
 *  1. [.navigate]s to the corresponding go rule instead of a directory.
 *
 *
 * Exactly one [BlazeGoPackage] per go rule.
 */
class BlazeGoPackage : GoPackage {

  private val id: BuildTargetIdentifier
  private val files: ConcurrentMap<File, Optional<PsiFile>> = ConcurrentHashMap()
  private val directories: ConcurrentMap<File, Optional<VirtualFile>> = ConcurrentHashMap()
  private val importPath: String
  private var importReferences: Array<PsiElement?>? = null

  constructor(project: Project, importPath: String, id: BuildTargetIdentifier) : super(
    project,
    getPackageName(project, getTargetToFileMap(project).get(id).toList(), importPath),
  ) {
    this.importPath = importPath
    this.id = replaceProtoLibrary(project, id)
    val files = getTargetToFileMap(project).get(id).toList()
    files.forEach { file ->
      this.files[file] = Optional.empty<PsiFile>()
    }
    files.mapNotNull { it.parentFile }.forEach { parent ->
      this.directories[parent] = Optional.empty<VirtualFile>()
    }
  }

  override fun getDirectories(): Set<VirtualFile> {
    directories.replaceAll { file, oldVirtualFile ->
      if (oldVirtualFile.filter(VirtualFile::isValid).isPresent) oldVirtualFile
      else Optional.ofNullable(BspVfsUtils.resolveVirtualFile(file, false))
    }
    return directories.values.stream().flatMap(Streams::stream).filter { it.isValid }.collect(toImmutableSet())
  }

  override fun getPsiDirectories(): StreamEx<PsiDirectory> {
    val psiManager = PsiManager.getInstance(project)
    return getDirectories()
      .mapNotNull { psiManager.findDirectory(it) }
      .filter { it.isValid }
      .let { StreamEx.of(it) }
  }

  override fun files(): Collection<PsiFile> {
    val psiManager = PsiManager.getInstance(project)
    files.replaceAll { file, oldGoFile ->
      if (oldGoFile.filter(PsiFile::isValid).isPresent) oldGoFile
      else Optional.ofNullable(BspVfsUtils.resolveVirtualFile(file, false)?.let { psiManager.findFile(it) }?.takeIf { it is GoFile })
    }
    return files.values.stream().flatMap(Streams::stream).filter { it.isValid }.collect(toImmutableSet())
  }

  override fun processFiles(processor: Processor<in PsiFile>, virtualFileFilter: Predicate<VirtualFile?>): Boolean {
    if (!isValid) return true

    val fileIndexFacade = FileIndexFacade.getInstance(project)
    for (file in files()) {
      ProgressIndicatorProvider.checkCanceled()
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
    val navigable = navigableElement
    if (navigable is Navigatable && navigable.canNavigate()) {
      navigable.navigate(requestFocus)
    }
  }

  override fun canNavigate(): Boolean {
    return navigableElement is Navigatable
  }

  override fun getNavigableElement(): PsiElement? {
    return null
//    val navigable = navigableElement
//    if (navigable != null && navigable.isValid) {
//      return navigable
//    }
//    val project = project
//    val buildReferenceManager = BuildReferenceManager.getInstance(project)
//    val resolveLabel = buildReferenceManager.resolveLabel(label)
//    return if (resolveLabel != null) {
//      navigableElement = resolveLabel
//      resolveLabel
//    } else {
//      navigableElement = buildReferenceManager.findBuildFile(
//        WorkspaceHelper.resolveBlazePackage(project, label),
//      )
//      navigableElement
//    }
  }

  override fun getCanonicalImport(contextModule: Module?): String {
    return this.importPath
  }

  override fun getImportPath(withVendoring: Boolean): String {
    return importPath
  }

  override fun getAllImportPaths(withVendoring: Boolean): Collection<String> {
    return listOf(importPath)
  }


  // need to override these because GoPackage uses GoPackage#myDirectories directly in their
  // implementations
  override fun isValid(): Boolean {
    return getDirectories().size == directories.size
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o !is BlazeGoPackage) {
      return false
    }
    val aPackage = o
    return importPath == aPackage.importPath && directories == aPackage.directories
  }

  override fun hashCode(): Int {
    return Objects.hash(importPath, directories)
  }

  override fun toString(): String {
    return "Package: " + this.importPath
  }

  /**
   * Returns a list of resolve targets, one for each import path component.
   *
   * <p>The final path component may either be the target name, or the containing directory name (in
   * which case the target name is usually go_default_library).
   *
   * <p>E.g., for target //foo:bar with import path "github.com/user/foo/bar"
   *
   * <pre>
   *   import "github.com/user/foo/bar"
   *           (1)        (2)  (3) (4)
   * </pre>
   *
   * <ol>
   *   <li>github.com resolves to nothing
   *   <li>user resolves to nothing
   *   <li>foo resolves to directory foo/
   *   <li>bar resolves to target //foo:bar
   * </ol>
   *
   * for target //one/two:go_default_library with import path "github.com/user/one/two"
   *
   * <pre>
   *    *   import "github.com/user/one/two"
   *    *           (1)        (2)  (3) (4)
   *    * </pre>
   *
   * <ol>
   *   <li>github.com resolves to nothing
   *   <li>user resolves to nothing
   *   <li>one resolves to directory one/
   *   <li>two resolves to directory one/two/
   * </ol>
   */
  fun getImportReferences(): Array<PsiElement?>? {
    val references = importReferences
    if (references != null && references.all { it?.isValid == true }) {
      return references
    }
    val navigable = navigableElement ?: return null
    return getImportReferences(this.id, navigable, importPath).also { importReferences = it }
  }

  fun getImportReferences(
    id: BuildTargetIdentifier,
    buildElement: PsiElement,
    importPath: String
  ): Array<PsiElement?> {
    val pathComponents = importPath.split("/")
    val importReferences = arrayOfNulls<PsiElement>(pathComponents.size)
    if (pathComponents.isEmpty()) {
      return importReferences
    }
    val lastElement = getLastElement(
      pathComponents.last(), id, buildElement,
    ) ?: return importReferences
    importReferences[importReferences.size - 1] = lastElement
    var currentElement = if (lastElement is PsiDirectory) lastElement.parent
    else {
      lastElement.containingFile?.parent
    }
    for (i in pathComponents.size - 2 downTo 0) {
      val name = currentElement?.name
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

  private fun getLastElement(
    name: String,
    id: BuildTargetIdentifier,
    buildElement: PsiElement
  ): PsiElement? {
    return null
//    return when (buildElement) {
//      is FuncallExpression -> {
//        if (name == label.targetName.toString()) {
//          buildElement
//        } else {
//          buildElement.containingFile
//            ?.parent
//            ?.takeIf { it.name == name }
//        }
//      }
//
//      is BuildFile -> {
//        val match = buildElement.parent
//        if (match != null && match.name == name) match else buildElement
//      }
//
//      else -> null
  }

  companion object {
    /**
     * Package name is determined by package declaration in the source files (must all be the same).
     *
     * - For `RuleTypes.GO_BINARY`, it will always be `main`.
     * - For `RuleTypes.GO_LIBRARY`, it is usually the last component of the import path
     *   (though not at all enforced).
     * - For `RuleTypes.GO_TEST`, it will be the same as the `RuleTypes.GO_BINARY`/`RuleTypes.GO_LIBRARY`
     *   under test, otherwise similar to a `RuleTypes.GO_LIBRARY` if no test subject is specified.
     * - For `RuleTypes.GO_PROTO_LIBRARY`, it's either declared via the `go_package` option,
     *   or automatically generated from the target name.
     */
    fun getPackageName(project: Project, files: List<File>, importPath: String): String {
      val psiManager = PsiManager.getInstance(project)
      return files.asSequence()
        .map { file -> BspVfsUtils.resolveVirtualFile(file, false) }
        .filterNotNull()
        .map { virtualFile -> psiManager.findFile(virtualFile) }
        .filterIsInstance<GoFile>()
        .filter { goFile -> goFile.buildFlags != "ignore" }
        .map { goFile -> goFile.canonicalPackageName } // strips _test suffix from test packages
        .filterNotNull()
        .firstOrNull() // short circuit
        ?: importPath.substringAfterLast('/')
    }

    /**
     * The import path for proto_library doesn't match the target name, we need to replace the
     * proto_library with the corresponding go_proto_library for them to match.
     */
    private fun replaceProtoLibrary(
      project: Project, targetId: BuildTargetIdentifier
    ): BuildTargetIdentifier {
      val targetMap = project.temporaryTargetUtils.targetsMap
      val target = targetMap[targetId] ?: return targetId
      val protoBuildTargetData = extractProtoBuildTarget(target) ?: return targetId

      if (protoBuildTargetData.ruleKind != "proto_library") {
        return targetId
      }

      return project.temporaryTargetUtils.targetDependentsGraph.directDependentIds(targetId)
        .asSequence()
        .mapNotNull { targetMap[it] }
        .filter { extractGoBuildTarget(it)?.ruleKind == "go_proto_library" }
        .map { it.id }
        .firstOrNull() ?: targetId
    }

    fun getTargetToFileMap(project: Project): MultiMap<BuildTargetIdentifier, File> =
      BspSyncCache.getInstance(project).getOrCompute(GO_TARGET_TO_FILE_MAP_KEY) {
        calculateTargetToFileMap(project)
      } as MultiMap<BuildTargetIdentifier, File>

    fun calculateTargetToFileMap(project: Project): MultiMap<BuildTargetIdentifier, File> {
      val targetMap = project.temporaryTargetUtils.targetsMap
      val goLibraries = project.temporaryTargetUtils.goLibraries
      val libraryToTestMap = buildLibraryToTestMap(project)

      val result = MultiMap<BuildTargetIdentifier, File>()

      for ((targetId, target) in targetMap) {
        val sourceFiles = getSourceFiles(target, libraryToTestMap)
          .asSequence()
          .mapNotNull { toRealFile(it) }
          .toSet()
        if (sourceFiles.isEmpty()) continue
        result.putValues(targetId, sourceFiles)
      }

      for (goLibrary in goLibraries) {
        val sourceFiles = goLibrary.goSources.mapNotNull { toRealFile(it.toPath().toFile()) }.toSet()
        result.putValues(goLibrary.id, sourceFiles)
      }

      return result
    }

    private fun buildLibraryToTestMap(project: Project): MultiMap<BuildTargetIdentifier, GoBuildTarget> {
      val targetMap = project.temporaryTargetUtils.targetsMap

      val result = MultiMap<BuildTargetIdentifier, GoBuildTarget>()

      for ((_, target) in targetMap) {
        val goBuildTargetData = extractGoBuildTarget(target) ?: continue
        if (!target.capabilities.canTest) continue
        if (goBuildTargetData.libraryLabels.isEmpty()) continue
        for (libraryLabel in goBuildTargetData.libraryLabels) {
          result.putValue(libraryLabel, goBuildTargetData)
        }

      }

      return result
    }

    private fun getSourceFiles(
      target: BuildTarget,
      libraryToTestMap: MultiMap<BuildTargetIdentifier, GoBuildTarget>
    ): Set<File> {
      val goBuildTargetData = extractGoBuildTarget(target) ?: return emptySet()
      return sequenceOf<GoBuildTarget>(goBuildTargetData)
        .plus(libraryToTestMap[target.id])
        .flatMap { it.generatedSources }
        .map { it.toPath().toFile() }
        .toSet()
    }

    /**
     * Workaround for https://github.com/bazelbuild/intellij/issues/2057. External workspace symlinks
     * can be changed externally by practically any bazel command. Such changes to symlinks will make
     * IntelliJ red. This helper resolves such symlink to an actual location.
     */
    private fun toRealFile(maybeExternal: File?): File? {
      maybeExternal ?: return null
      // do string manipulation instead of .toPath().toRealPath().toFile()
      // because there might be a race condition and symlink won't be resolved at the time
      val externalString = maybeExternal.toString()
      return if (externalString.contains("/external/") &&
        !externalString.contains("/bazel-out/") &&
        !externalString.contains("/blaze-out/")
      ) {
        File(externalString.replace("/execroot.*?/external/".toRegex(), "/external/"))
      } else {
        maybeExternal
      }
    }
  }
}
