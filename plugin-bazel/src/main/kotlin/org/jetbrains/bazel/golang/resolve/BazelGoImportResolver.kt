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

import com.goide.psi.impl.GoPackage
import com.goide.psi.impl.imports.GoImportReference
import com.goide.psi.impl.imports.GoImportResolver
import com.google.common.collect.ImmutableList
import com.intellij.codeInsight.navigation.CtrlMouseHandler
import com.intellij.lang.documentation.DocumentationProviderEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.SyntheticFileSystemItem
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.util.ThreeState
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import java.util.concurrent.ConcurrentHashMap

private const val GO_PACKAGE_MAP_KEY = "BazelGoPackageMap"
private const val GO_TARGET_MAP_KEY = "BazelGoTargetMap"

/** Converts each go target in the [targetUtils#allBuildTargets()] into a corresponding [BazelGoPackage]. */
class BazelGoImportResolver : GoImportResolver {
  override fun resolve(
    importPath: String,
    project: Project,
    module: Module?,
    resolveState: ResolveState?,
  ): Collection<GoPackage>? {
    val goPackage = doResolve(importPath, project)
    return if (goPackage != null) ImmutableList.of(goPackage) else null
  }

  override fun supportsRelativeImportPaths(project: Project, module: Module?): ThreeState = ThreeState.NO

  override fun resolve(reference: GoImportReference): Array<ResolveResult>? {
    val importPath = reference.fileReferenceSet.pathString
    val project = reference.element.project
    val goPackage = doResolve(importPath, project) ?: return null
    return doResolve(goPackage, reference.index)
  }
}

/**
 * [GoImportReference] must resolve to a [PsiFileSystemItem], but we might want it to
 * resolve to a build rule in a [BuildFile]. We'll just return the [BuildFile] with a
 * navigation redirect.
 */
private class GoPackageFileSystemItem private constructor(private val name: String, private val rule: StarlarkCallExpression) :
  SyntheticFileSystemItem(rule.project) {
    companion object {
      @JvmStatic
      fun getInstance(element: PsiElement): PsiFileSystemItem? =
        when (element) {
          is PsiFileSystemItem -> element
          is StarlarkCallExpression -> element.name?.let { GoPackageFileSystemItem(it, element) }
          else -> null
        }
    }

    override fun getName(): String = name

    override fun getNavigationElement(): PsiElement = rule

    override fun getParent(): PsiFileSystemItem? = (rule.containingFile as? StarlarkFile)?.takeIf { it.isBuildFile() }?.parent

    override fun getVirtualFile(): VirtualFile? = null

    override fun isValid(): Boolean = true

    override fun isPhysical(): Boolean = false

    override fun isDirectory(): Boolean = false

    override fun processChildren(psiElementProcessor: PsiElementProcessor<in PsiFileSystemItem>): Boolean = false
  }

/** Redirects quick navigation text on the fake file system item back to the build rule. */
class GoPackageDocumentationProvider : DocumentationProviderEx() {
  override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String? =
    if (element is GoPackageFileSystemItem) {
      CtrlMouseHandler.getInfo(element.navigationElement, originalElement)
    } else {
      null
    }
}

fun doResolve(importPath: String, project: Project): BazelGoPackage? {
  if (!project.isBazelProject) return null
  val targetUtils = project.targetUtils

  val goPackageMap = getGoPackageMap(project)
  val goTargetMap = getGoTargetMap(project)
  val targetLabel = goTargetMap[importPath] ?: return null
  val target = targetUtils.getBuildTargetForLabel(targetLabel) ?: return null
  return goPackageMap
    .computeIfAbsent(importPath) { path -> BazelGoPackage(project = project, importPath = path, target = target) }
}

fun getGoPackageMap(project: Project): ConcurrentHashMap<String, BazelGoPackage> =
  SyncCache
    .getInstance(project)
    .getOrCompute(GO_PACKAGE_MAP_KEY) { ConcurrentHashMap<String, BazelGoPackage>() } as ConcurrentHashMap<String, BazelGoPackage>

private fun getGoTargetMap(project: Project): Map<String, Label> =
  SyncCache
    .getInstance(project)
    .getOrCompute(GO_TARGET_MAP_KEY) {
      val targetUtils = project.targetUtils
      targetUtils
        .allBuildTargets()
        .filter { t -> extractGoBuildTarget(t)?.importPath?.isNotBlank() == true }
        .groupBy { t -> extractGoBuildTarget(t)?.importPath.orEmpty() }
        .mapValues { (_, targets) ->
          // duplicates are possible (e.g., same target with different aspects)
          // choose the one with the most sources (though they're probably the same)
          targets.maxByOrNull { extractGoBuildTarget(it)?.generatedSources?.size ?: 0 }?.id
        }
    } as Map<String, Label>

fun doResolve(goPackage: BazelGoPackage, index: Int): Array<ResolveResult> =
  listOf(goPackage)
    .asSequence()
    .mapNotNull { it.getImportReferences() }
    .filter { index < it.size }
    .mapNotNull { it[index] }
    .mapNotNull { GoPackageFileSystemItem.getInstance(it) }
    .map { PsiElementResolveResult(it) }
    .toList()
    .toTypedArray()
