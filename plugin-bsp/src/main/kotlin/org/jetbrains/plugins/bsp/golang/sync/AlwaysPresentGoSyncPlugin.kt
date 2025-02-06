///*
// * Copyright 2017 The Bazel Authors. All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
package org.jetbrains.plugins.bsp.golang.sync
//
//import com.google.idea.blaze.base.model.BlazeProjectData
//
///**
// * Unlike most of the go-specific code, will be run even if the JetBrains Go plugin isn't enabled.
// */
//class AlwaysPresentGoSyncPlugin : BlazeSyncPlugin {
//  public override fun getSupportedLanguagesInWorkspace(workspaceType: WorkspaceType?): MutableSet<LanguageClass?> {
//    return if (com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.isGoPluginSupported()) com.google.common.collect.ImmutableSet.of<LanguageClass?>(
//        LanguageClass.GO,
//    ) else com.google.common.collect.ImmutableSet.of<LanguageClass?>()
//  }
//
//  public override fun getRequiredExternalPluginIds(languages: MutableCollection<LanguageClass?>): com.google.common.collect.ImmutableList<String?> {
//    return if (languages.contains(LanguageClass.GO))
//      com.google.common.collect.ImmutableList.of<String?>(com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.GO_PLUGIN_ID)
//    else
//      com.google.common.collect.ImmutableList.of<String?>()
//  }
//
//  public override fun validate(
//    project: com.intellij.openapi.project.Project?, context: BlazeContext?, blazeProjectData: BlazeProjectData
//  ): Boolean {
//    if (!blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.GO)
//      || PluginUtils.isPluginEnabled(com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.GO_PLUGIN_ID)
//    ) {
//      return true
//    }
//    if (PluginUtils.isPluginEnabled(com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.OLD_GO_PLUGIN_ID)) {
//      val error: String? =
//        java.lang.String.format(
//            "The currently installed Go plugin is no longer supported by the %s plugin.\n"
//              + "Click here to install the new JetBrains Go plugin and restart.",
//            Blaze.defaultBuildSystemName(),
//        )
//      IssueOutput.error(error)
//        .withNavigatable(
//            object : com.intellij.pom.NavigatableAdapter() {
//                override fun navigate(requestFocus: Boolean) {
//                    com.intellij.ide.plugins.PluginManager.disablePlugin(com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.OLD_GO_PLUGIN_ID)
//                    PluginUtils.installOrEnablePlugin(com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.GO_PLUGIN_ID)
//                }
//            },
//        )
//        .submit(context)
//      return true
//    }
//    IssueOutput.error(
//        "Go support requires the Go plugin. Click here to install/enable the JetBrains Go "
//          + "plugin, then restart the IDE",
//    )
//      .withNavigatable(PluginUtils.installOrEnablePluginNavigable(com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.GO_PLUGIN_ID))
//      .submit(context)
//    return true
//  }
//
//  public override fun validateProjectView(
//    project: com.intellij.openapi.project.Project?,
//    context: BlazeContext?,
//    projectViewSet: ProjectViewSet,
//    workspaceLanguageSettings: WorkspaceLanguageSettings
//  ): Boolean {
//    if (workspaceLanguageSettings.isLanguageActive(LanguageClass.GO) && !com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.isGoPluginSupported()) {
//      val msg: String? =
//        java.lang.String.format(
//            "Go is no longer supported by the %s plugin with IntelliJ Community Edition.\n"
//              + "Please install Ultimate Edition and upgrade to the JetBrains Go plugin",
//            Blaze.defaultBuildSystemName(),
//        )
//      IssueOutput.error(msg).submit(context)
//      BlazeSyncManager.printAndLogError(msg, context)
//      return false
//    }
//    if (com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.goWorkspaceTypeSupported()
//      || !workspaceLanguageSettings.isWorkspaceType(WorkspaceType.GO)
//    ) {
//      return true
//    }
//    val topLevelProjectViewFile: ProjectViewFile? = projectViewSet.getTopLevelProjectViewFile()
//    var msg =
//      ("Go workspace_type is no longer supported. Please add 'go' to "
//        + "additional_languages instead")
//    val fixable =
//      project != null && topLevelProjectViewFile != null && (topLevelProjectViewFile.projectView.getScalarValue(
//          WorkspaceTypeSection.KEY,
//      )
//        === WorkspaceType.GO)
//    BlazeSyncManager.printAndLogError(msg, context)
//    msg += if (fixable) ". Click here to fix your .blazeproject and resync." else ", then resync."
//    IssueOutput.error(msg)
//      .withNavigatable(
//          if (!fixable)
//              null
//          else
//              object : com.intellij.pom.NavigatableAdapter() {
//                  override fun navigate(requestFocus: Boolean) {
//                      com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.fixLanguageSupport(
//                          project,
//                      )
//                  }
//              },
//      )
//      .submit(context)
//    return false
//  }
//
//  companion object {
//    private const val GO_PLUGIN_ID = "org.jetbrains.plugins.go"
//    private const val OLD_GO_PLUGIN_ID = "ro.redeul.google.go"
//
//    private val isGoPluginSupported: Boolean
//      /** Go plugin is only supported in IJ UE and GoLand.  */
//      get() = com.intellij.util.PlatformUtils.isGoIde()
//        || com.intellij.util.PlatformUtils.isIdeaUltimate()
//        || com.intellij.openapi.application.ApplicationManager.getApplication().isUnitTestMode()
//
//    private fun fixLanguageSupport(project: com.intellij.openapi.project.Project?) {
//      val edit: ProjectViewEdit? =
//        ProjectViewEdit.editLocalProjectView(
//            project,
//            { builder ->
//                com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.removeGoWorkspaceType(
//                    builder,
//                )
//                com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.addToAdditionalLanguages(
//                    builder,
//                )
//                true
//            },
//        )
//      if (edit == null) {
//        com.intellij.openapi.ui.Messages.showErrorDialog(
//            "Could not modify project view. Check for errors in your project view and try again",
//            "Error",
//        )
//        return
//      }
//      edit.apply()
//
//      BlazeSyncManager.getInstance(project)
//        .incrementalProjectSync( /* reason= */"enabled-go-support")
//    }
//
//    private fun goWorkspaceTypeSupported(): Boolean {
//      return com.intellij.util.PlatformUtils.isGoIde()
//    }
//
//    private fun removeGoWorkspaceType(builder: ProjectView.Builder) {
//      if (com.google.idea.blaze.golang.sync.AlwaysPresentGoSyncPlugin.Companion.goWorkspaceTypeSupported()) {
//        return
//      }
//      val section: ScalarSection<WorkspaceType?>? = builder.getLast(WorkspaceTypeSection.KEY)
//      if (section != null && section.getValue() === WorkspaceType.GO) {
//        builder.remove(section)
//      }
//    }
//
//    private fun addToAdditionalLanguages(builder: ProjectView.Builder) {
//      val existingSection: ListSection<LanguageClass?>? = builder.getLast(AdditionalLanguagesSection.KEY)
//      builder.replace(
//          existingSection,
//          ListSection.update(AdditionalLanguagesSection.KEY, existingSection).add(LanguageClass.GO),
//      )
//    }
//  }
//}
