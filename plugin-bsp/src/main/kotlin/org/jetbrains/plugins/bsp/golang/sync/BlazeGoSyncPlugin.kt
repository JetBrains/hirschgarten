/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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

import com.google.idea.blaze.base.model.BlazeProjectData

/** Supports golang.  */
class BlazeGoSyncPlugin : BlazeSyncPlugin {
  public override fun getSupportedLanguagesInWorkspace(workspaceType: WorkspaceType?): MutableSet<LanguageClass?> {
    return com.google.common.collect.ImmutableSet.of<LanguageClass?>(LanguageClass.GO)
  }

  val defaultWorkspaceType: WorkspaceType?
    get() = if (com.intellij.util.PlatformUtils.isGoIde()) WorkspaceType.GO else null

  val supportedWorkspaceTypes: com.google.common.collect.ImmutableList<WorkspaceType?>
    get() = if (com.intellij.util.PlatformUtils.isGoIde()) com.google.common.collect.ImmutableList.of<WorkspaceType?>(
      WorkspaceType.GO,
    ) else com.google.common.collect.ImmutableList.of<WorkspaceType?>()

  public override fun getSourceFolderProvider(projectData: BlazeProjectData): SourceFolderProvider? {
    if (!projectData.getWorkspaceLanguageSettings().isWorkspaceType(WorkspaceType.GO)) {
      return null
    }
    return GenericSourceFolderProvider.INSTANCE
  }

  public override fun getWorkspaceModuleType(workspaceType: WorkspaceType?): com.intellij.openapi.module.ModuleType<*>? {
    return if (workspaceType === WorkspaceType.GO)
      com.intellij.openapi.module.ModuleTypeManager.getInstance().getDefaultModuleType()
    else
      null
  }

  public override fun updateProjectStructure(
    project: com.intellij.openapi.project.Project,
    context: BlazeContext?,
    workspaceRoot: WorkspaceRoot?,
    projectViewSet: ProjectViewSet?,
    blazeProjectData: BlazeProjectData,
    oldBlazeProjectData: BlazeProjectData?,
    moduleEditor: ModuleEditor?,
    workspaceModule: com.intellij.openapi.module.Module?,
    workspaceModifiableModel: com.intellij.openapi.roots.ModifiableRootModel
  ) {
    if (!blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.GO)) {
      return
    }
    for (lib in com.google.idea.blaze.golang.sync.BlazeGoSyncPlugin.Companion.getGoLibraries(project)) {
      if (workspaceModifiableModel.findLibraryOrderEntry(lib) == null) {
        workspaceModifiableModel.addLibraryEntry(lib)
      }
    }
    com.intellij.ide.util.PropertiesComponent.getInstance().setValue(
      com.google.idea.blaze.golang.sync.BlazeGoSyncPlugin.Companion.DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH,
      true,
    )
  }

  public override fun getLibrarySource(
    projectViewSet: ProjectViewSet?, blazeProjectData: BlazeProjectData
  ): LibrarySource? {
    if (!blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.GO)) {
      return null
    }
    return BlazeGoLibrarySource.INSTANCE
  }

  companion object {
    /** From [com.goide.inspections.WrongSdkConfigurationNotificationProvider].  */
    private const val DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH = "DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH"

    val GO_LIBRARY_PREFIXES: com.google.common.collect.ImmutableSet<String?> =
      com.google.common.collect.ImmutableSet.of<String?>("GOPATH", "Go SDK")

    private fun getGoLibraries(project: com.intellij.openapi.project.Project): MutableList<com.intellij.openapi.roots.libraries.Library> {
      val libraries: MutableList<com.intellij.openapi.roots.libraries.Library> =
        com.google.common.collect.Lists.newArrayList<com.intellij.openapi.roots.libraries.Library?>()
      val registrar: com.intellij.openapi.roots.libraries.LibraryTablesRegistrar =
        com.intellij.openapi.roots.libraries.LibraryTablesRegistrar.getInstance()
      for (lib in registrar.getLibraryTable().getLibraries()) {
        if (BlazeGoLibrarySource.isGoLibrary(lib)) {
          libraries.add(lib)
        }
      }

      for (lib in registrar.getLibraryTable(project).getLibraries()) {
        if (BlazeGoLibrarySource.isGoLibrary(lib)) {
          libraries.add(lib)
        }
      }
      return libraries
    }
  }
}
