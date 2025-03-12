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
package org.jetbrains.bazel.ogRun.exporter

import com.google.idea.blaze.base.actions.BlazeProjectAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 * Export selected run configurations to file, so they can be checked in and shared between users.
 */
class ExportRunConfigurationAction :
  BlazeProjectAction(),
  DumbAware {
  protected override fun querySyncSupport(): QuerySyncStatus = QuerySyncStatus.SUPPORTED

  protected override fun updateForBlazeProject(project: Project, e: AnActionEvent?) {
    ActionPresentationHelper
      .of(e)
      .disableIf(getInstance.getInstance(project).allConfigurationsList.isEmpty())
      .commit()
  }

  protected override fun actionPerformedInBlazeProject(project: Project?, e: AnActionEvent?) {
    ExportRunConfigurationDialog(project).show()
  }
}
