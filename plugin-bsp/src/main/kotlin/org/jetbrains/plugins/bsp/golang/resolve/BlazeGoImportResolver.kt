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

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.android.utils.mapValuesNotNull
import com.goide.psi.impl.GoPackage
import com.goide.psi.impl.imports.GoImportReference
import com.goide.psi.impl.imports.GoImportResolver
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.util.ThreeState
import com.jetbrains.rd.util.ConcurrentHashMap
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.impl.projectAware.BspSyncCache
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils

/** Converts each go target in the [temporaryTargetUtils.targetsMap] into a corresponding [BlazeGoPackage].  */
internal class BlazeGoImportResolver : GoImportResolver {
  override fun resolve(
    importPath: String,
    project: Project,
    module: Module?,
    resolveState: ResolveState?
  ): List<GoPackage>? {
    val goPackage = doResolve(importPath, project)
    return if (goPackage != null) listOf(goPackage) else null
  }

  override fun supportsRelativeImportPaths(
    project: Project,
    module: Module?
  ): ThreeState = ThreeState.NO

  override fun resolve(reference: GoImportReference): Array<ResolveResult>? {
    val importPath = reference.fileReferenceSet.pathString
    val project = reference.element.project
    val goPackage: BlazeGoPackage = doResolve(importPath, project) ?: return null
    return doResolve(goPackage, reference.index)
  }

  fun doResolve(goPackage: BlazeGoPackage, index: Int): Array<ResolveResult> {
    val importReferences = goPackage.getImportReferences()?.takeIf { index < it.size } ?: return emptyArray()
    val element = importReferences[index] as? PsiFileSystemItem ?: return emptyArray()
    return arrayOf(PsiElementResolveResult(element))
  }

  companion object {
    private const val GO_PACKAGE_MAP_KEY = "BlazeGoPackageMap"
    private const val GO_TARGET_MAP_KEY = "BlazeGoTargetMap"

    fun getGoPackageMap(project: Project): ConcurrentHashMap<String, BlazeGoPackage> {
      return BspSyncCache.getInstance(project)
        .getOrCompute(
          GO_PACKAGE_MAP_KEY,
        ) { ConcurrentHashMap<String, BlazeGoPackage>() } as ConcurrentHashMap<String, BlazeGoPackage>
    }

    private fun getGoTargetMap(project: Project): Map<String, BuildTargetIdentifier> {
      return BspSyncCache.getInstance(project)
        .getOrCompute(
          GO_TARGET_MAP_KEY,
        ) {
          val targetMap = project.temporaryTargetUtils.targetsMap
          targetMap
            .mapValuesNotNull { extractGoBuildTarget(it.value) }
            .filter { it.value.importPath != null }
            .map { it.value.importPath to it.key }.toMap()
        } as Map<String, BuildTargetIdentifier>
    }


    fun doResolve(importPath: String, project: Project): BlazeGoPackage? {
      val goPackageMap = getGoPackageMap(project)
      val goTargetMap = getGoTargetMap(project)
      val targetKey = goTargetMap[importPath] ?: return null
      return goPackageMap
        .computeIfAbsent(
          importPath,
        ) { path: String ->
          BlazeGoPackage(
            project,
            path,
            targetKey,
          )
        }
    }
  }
}
