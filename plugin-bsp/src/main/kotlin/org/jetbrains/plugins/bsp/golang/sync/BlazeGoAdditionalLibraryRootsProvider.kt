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
package org.jetbrains.plugins.bsp.golang.sync

/** Provides out-of-project go sources for indexing.  */
class BlazeGoAdditionalLibraryRootsProvider : BlazeExternalLibraryProvider() {
  protected val libraryName: String
    get() = com.google.idea.blaze.golang.sync.BlazeGoAdditionalLibraryRootsProvider.Companion.GO_EXTERNAL_LIBRARY_ROOT_NAME

  protected override fun getLibraryFiles(
    project: com.intellij.openapi.project.Project?,
    projectData: BlazeProjectData
  ): com.google.common.collect.ImmutableList<java.io.File?>? {
    val importRoots: ImportRoots? = ImportRoots.forProjectSafe(project)
    return if (importRoots != null)
      com.google.idea.blaze.golang.sync.BlazeGoAdditionalLibraryRootsProvider.Companion.getLibraryFiles(
        project,
        projectData,
        importRoots,
      )
    else
      com.google.common.collect.ImmutableList.of<java.io.File?>()
  }

  companion object {
    const val GO_EXTERNAL_LIBRARY_ROOT_NAME: String = "Go Libraries"

    fun getLibraryFiles(
      project: com.intellij.openapi.project.Project?, projectData: BlazeProjectData, importRoots: ImportRoots
    ): com.google.common.collect.ImmutableList<java.io.File?>? {
      if (!projectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.GO)) {
        return com.google.common.collect.ImmutableList.of<java.io.File?>()
      }
      val workspaceRoot: WorkspaceRoot? = WorkspaceRoot.fromProjectSafe(project)
      if (workspaceRoot == null) {
        return com.google.common.collect.ImmutableList.of<java.io.File?>()
      }
      val isExternal: java.util.function.Predicate<java.io.File?> =
        java.util.function.Predicate { f: java.io.File? ->
          val path: WorkspacePath? = workspaceRoot.workspacePathForSafe(f)
          path == null || !importRoots.containsWorkspacePath(path)
        }
      // don't use sync cache, because
      // 1. this is used during sync before project data is saved
      // 2. the roots provider is its own cache
      return BlazeGoPackage.getUncachedTargetToFileMap(project, projectData).values().stream()
        .filter(isExternal)
        .filter({ f -> f.getName().endsWith(".go") })
        .distinct()
        .collect(com.google.common.collect.ImmutableList.toImmutableList<E?>())
    }
  }
}
