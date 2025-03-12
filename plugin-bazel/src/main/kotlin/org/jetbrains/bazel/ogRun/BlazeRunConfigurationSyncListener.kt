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
package org.jetbrains.bazel.ogRun

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.google.idea.blaze.base.model.BlazeProjectData
import com.intellij.execution.*
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import java.io.File
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors

/**
 * Imports run configurations specified in the project view, and creates run configurations for
 * project view targets, where appropriate.
 */
class BlazeRunConfigurationSyncListener : SyncListener {
  public override fun onSyncComplete(
    project: Project,
    context: BlazeContext?,
    importSettings: BlazeImportSettings?,
    projectViewSet: ProjectViewSet,
    buildIds: ImmutableSet<Int?>?,
    blazeProjectData: BlazeProjectData,
    syncMode: SyncMode?,
    syncResult: SyncResult?,
  ) {
    updateExistingRunConfigurations(project)
    removeInvalidRunConfigurations(project)
    if (syncMode === SyncMode.STARTUP || syncMode === SyncMode.NO_BUILD) {
      return
    }

    val xmlFiles: MutableSet<File?> =
      getImportedRunConfigurations(projectViewSet, blazeProjectData.getWorkspacePathResolver())
    Transactions.submitTransactionAndWait(
      {
        // First, import from specified XML files. Then auto-generate from targets.
        xmlFiles.forEach(
          Consumer { file: File? -> RunConfigurationSerializer.loadFromXmlIgnoreExisting(project, file) },
        )

        val labelsWithConfigs: MutableSet<Label?> = labelsWithConfigs(project)
        val targetExpressions: MutableSet<Label?> =
          Sets.newLinkedHashSet<E?>(projectViewSet.listItems(TargetSection.KEY))
        // We only auto-generate configurations for rules listed in the project view.
        for (target in targetExpressions) {
          if (target !is Label || labelsWithConfigs.contains(target)) {
            continue
          }
          val label: Label? = target as Label?
          labelsWithConfigs.add(label)
          maybeAddRunConfiguration(project, blazeProjectData, label)
        }
      },
    )
  }

  companion object {
    private fun removeInvalidRunConfigurations(project: Project) {
      val manager: RunManagerImpl = getInstanceImpl.getInstanceImpl(project)
      val toRemove: MutableList<RunnerAndConfigurationSettings?> =
        manager
          .getConfigurationSettingsList(BlazeCommandRunConfigurationType.getInstance())
          .filter { s -> isInvalidRunConfig(s!!.configuration) }
          .toMutableList()
      if (!toRemove.isEmpty()) {
        manager.removeConfigurations(toRemove)
      }
    }

    private fun isInvalidRunConfig(config: RunConfiguration?): Boolean =
      config is BlazeCommandRunConfiguration &&
        config.pendingSetupFailed()

    /**
     * On each sync, re-calculate target kind for all existing run configurations, in case the target
     * map has changed since the last sync. Also force-enable our before-run task on all
     * configurations.
     */
    private fun updateExistingRunConfigurations(project: Project) {
      val manager: RunManagerImpl = getInstanceImpl.getInstanceImpl(project)
      var beforeRunTasksChanged = false
      for (config in manager.getConfigurationsList(BlazeCommandRunConfigurationType.instance)) {
        if (config is BlazeCommandRunConfiguration) {
          config.updateTargetKindAsync({ })
          beforeRunTasksChanged = beforeRunTasksChanged or enableBlazeBeforeRunTask(config)
        }
      }
      if (beforeRunTasksChanged) {
        manager.fireBeforeRunTasksUpdated()
      }
    }

    private fun enableBlazeBeforeRunTask(config: BlazeCommandRunConfiguration): Boolean {
      val tasks =
        RunManagerEx.getInstanceEx(config.project).getBeforeRunTasks(config)
      if (tasks.none { it.getProviderId() == BlazeBeforeRunTaskProvider.ID }
      ) {
        return addBlazeBeforeRunTask(config)
      }
      var changed = false
      for (task in tasks) {
        if (task.getProviderId() == BlazeBeforeRunTaskProvider.ID && !task.isEnabled) {
          changed = true
          task.isEnabled = true
        }
      }
      return changed
    }

    private fun addBlazeBeforeRunTask(config: BlazeCommandRunConfiguration): Boolean {
      val provider: BeforeRunTaskProvider<*>? =
        BlazeBeforeRunTaskProvider.getProvider<BlazeBeforeRunTaskProvider.Task?>(
          config.getProject(),
          BlazeBeforeRunTaskProvider.ID,
        )
      if (provider == null) {
        return false
      }
      val task: BeforeRunTask<*>? = provider.createTask(config)
      if (task == null) {
        return false
      }
      task.setEnabled(true)

      val beforeRunTasks: MutableList<BeforeRunTask<*>?> =
        ArrayList<BeforeRunTask<*>?>(config.getBeforeRunTasks())
      beforeRunTasks.add(task)
      config.setBeforeRunTasks(beforeRunTasks)

      return true
    }

    private fun getImportedRunConfigurations(projectViewSet: ProjectViewSet, pathResolver: WorkspacePathResolver): MutableSet<File?> =
      projectViewSet
        .listItems(RunConfigurationsSection.KEY)
        .stream()
        .map(pathResolver::resolveToFile)
        .collect(Collectors.toCollection(Supplier { LinkedHashSet() }))

    /**
     * Collects a set of all the Blaze labels that have an associated run configuration.
     */
    private fun labelsWithConfigs(project: Project): MutableSet<Label?> {
      val configurations: MutableList<RunConfiguration?> =
        getInstance.getInstance(project).allConfigurationsList
      val labelsWithConfigs: MutableSet<Label?> = Sets.newHashSet<Label?>()
      for (configuration in configurations) {
        if (configuration is BlazeRunConfiguration) {
          val config = configuration as BlazeRunConfiguration
          config.targets
            .stream()
            .filter { t: Label? -> t is Label }
            .map<Any?> { t: Label? -> t as Label }
            .forEach { e: Any? -> labelsWithConfigs.add(e) }
        }
      }
      return labelsWithConfigs
    }

    /**
     * Adds a run configuration for an android_binary target if there is not already a configuration
     * for that target.
     */
    private fun maybeAddRunConfiguration(
      project: Project,
      blazeProjectData: BlazeProjectData,
      label: Label?,
    ) {
      val runManager: RunManager = getInstance.getInstance(project)

      for (configurationFactory in BlazeRunConfigurationFactory.EP_NAME.extensions) {
        if (configurationFactory.handlesTarget(project, blazeProjectData, label)) {
          val settings =
            configurationFactory.createForTarget(project, runManager, label)
          runManager.addConfiguration(settings, /* isShared= */false)
          if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = settings
          }
          break
        }
      }
    }
  }
}
