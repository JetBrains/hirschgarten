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

import com.goide.project.GoPackageFactory
import com.goide.psi.GoFile
import com.goide.psi.impl.GoPackage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.impl.projectAware.BspSyncCache
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private const val BLAZE_GO_PACKAGE_FACTORY_CACHE_ID = "BlazeGoPackageFactory"

/** Updates and exposes a map of import paths to files.  */
class BlazeGoPackageFactory : GoPackageFactory {
  override fun createPackage(goFile: GoFile): GoPackage? {
    val virtualFile = goFile.getVirtualFile() ?: return null
    val project = goFile.project
    val fileToImportPathMap = getFileToImportPathMap(project) ?: return null
    val importPath = fileToImportPathMap[VfsUtil.virtualToIoFile(virtualFile)] ?: return null
    return BlazeGoImportResolver.doResolve(importPath, project)
  }

  override fun createPackage(packageName: String, vararg directories: PsiDirectory?): GoPackage? = null

  companion object {

    fun getFileToImportPathMap(project: Project): ConcurrentMap<File, String>? {
      return BspSyncCache.getInstance(project)
        .getOrCompute(
          BLAZE_GO_PACKAGE_FACTORY_CACHE_ID,
        ) {
          buildFileToImportPathMap(
            project,
          )
        } as ConcurrentMap<File, String>?
    }

    private fun buildFileToImportPathMap(project: Project): ConcurrentMap<File, String> {
      val targetMap = project.temporaryTargetUtils.targetsMap
      val map = ConcurrentHashMap<File, String>()
      val targetToFile = BlazeGoPackage.getTargetToFileMap(project)
      for ((_, target) in targetMap) {
        val goBuildTargetData = extractGoBuildTarget(target) ?: continue
        val importPath =
          goBuildTargetData.libraryLabels.asSequence().mapNotNull { targetMap[it] }.mapNotNull { extractGoBuildTarget(it) }
            .mapNotNull { it.importPath }
            .firstOrNull() ?: goBuildTargetData.importPath ?: continue
        for (file in targetToFile.get(target.id)) {
          map.putIfAbsent(file, importPath)
        }
      }
      return map
    }
  }
}
