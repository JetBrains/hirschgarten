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

import com.goide.project.GoPackageFactory
import com.goide.psi.GoFile
import com.goide.psi.impl.GoPackage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal const val GO_PACKAGE_FACTORY_KEY = "BazelGoPackageFactory"

/** Updates and exposes a map of import paths to files. */
class BazelGoPackageFactory : GoPackageFactory {
  override fun createPackage(goFile: GoFile): GoPackage? {
    if (!BazelFeatureFlags.isGoSupportEnabled) return null
    val project = goFile.project
    if (!project.isBazelProject) return null
    val virtualFile = goFile.virtualFile ?: return null
    val fileToImportPathMap = getFileToImportPathMap(project)
    val importPath = fileToImportPathMap[virtualFile.toNioPath()]
    return if (importPath != null) doResolve(importPath, project) else null
  }

  override fun createPackage(packageName: String, vararg directories: PsiDirectory): GoPackage? = null

  companion object {
    @JvmStatic
    fun getFileToImportPathMap(project: Project): ConcurrentMap<Path, String> =
      SyncCache
        .getInstance(project)
        .getOrCompute(GO_PACKAGE_FACTORY_KEY) { buildFileToImportPathMap(project) } as ConcurrentMap<Path, String>

    private fun buildFileToImportPathMap(project: Project): ConcurrentMap<Path, String> {
      val targetUtils = project.targetUtils
      val map = ConcurrentHashMap<Path, String>()
      val targetToFile = BazelGoPackage.getTargetToFileMap(project)

      for (target in targetUtils.allBuildTargets()) {
        val goBuildTarget = extractGoBuildTarget(target) ?: continue

        val importPath =
          goBuildTarget.libraryLabels
            .asSequence()
            .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
            .mapNotNull { extractGoBuildTarget(it) }
            .map { it.importPath }
            .firstOrNull() ?: goBuildTarget.importPath.takeIf { it.isNotBlank() } ?: continue

        for (file in targetToFile.get(target.id)) {
          map.putIfAbsent(file, importPath)
        }
      }

      return map
    }
  }
}
