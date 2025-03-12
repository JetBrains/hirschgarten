/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.smrunner

import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.intellij.execution.Location
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import java.util.stream.Stream

/**
 * Finds the corresponding wrapped test for a web_test and recreates the test URL with the correct
 * [BlazeTestEventsHandler], then locates the test elements using the corresponding [ ].
 */
internal class BlazeWebTestLocator : SMTestLocator {
  override fun getLocation(
    protocol: String?,
    path: String,
    project: Project,
    scope: GlobalSearchScope,
  ): MutableList<Location<*>?> {
    if (protocol != BlazeWebTestEventsHandler.WEB_TEST_PROTOCOL) {
      return ImmutableList.of<Location<*>?>()
    }
    val projectData: BlazeProjectData? =
      BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
    if (projectData == null) {
      return ImmutableList.of<Location<*>?>()
    }
    val components = Splitter.on(SmRunnerUtils.TEST_NAME_PARTS_SPLITTER).splitToList(path)
    if (components.isEmpty()) {
      return ImmutableList.of<Location<*>?>()
    }
    val wrapperLabel: Label? = Label.createIfValid(components.get(0))
    if (wrapperLabel == null) {
      return ImmutableList.of<Location<*>?>()
    }
    val wrapperTarget: TargetIdeInfo? =
      projectData.getTargetMap().get(TargetKey.forPlainTarget(wrapperLabel))
    if (wrapperTarget == null) {
      return ImmutableList.of<Location<*>?>()
    }
    val builder = ImmutableList.builder<Location<*>?>()
    for (dependency in wrapperTarget.getDependencies()) {
      val targetKey: TargetKey = dependency.getTargetKey()
      val target: TargetIdeInfo? = projectData.getTargetMap().get(targetKey)
      if (target == null) {
        continue
      }
      val kind: Kind = target.getKind()
      val label: Label = targetKey.getLabel()
      if (Stream.of<String?>("_wrapped_test", "_debug").noneMatch(label.targetName().toString()::endsWith)) {
        continue
      }
      val handler =
        BlazeTestEventsHandler.getHandlerForTargetKind(kind).orElse(null)
      if (handler == null || handler.testLocator == null) {
        continue
      }
      val url: String? = recreateUrl(handler, label, kind, components)
      if (url == null) {
        continue
      }
      builder.addAll(locate(handler.testLocator, url, project, scope))
    }
    return builder.build()
  }

  companion object {
    val INSTANCE: BlazeWebTestLocator = BlazeWebTestLocator()

    private fun recreateUrl(
      handler: BlazeTestEventsHandler,
      label: Label,
      kind: Kind,
      components: MutableList<String?>,
    ): String? {
      when (components.size) {
        2 -> return handler.suiteLocationUrl(label, kind, /* name = */components.get(1))
        4 -> return handler.testLocationUrl(
          label,
          kind, // parentSuite =
          components.get(1), // name =
          components.get(2), // className =
          Strings.emptyToNull(components.get(3)),
        )

        else -> return null
      }
    }

    private fun locate(
      locator: SMTestLocator,
      url: String,
      project: Project,
      scope: GlobalSearchScope,
    ): MutableList<Location<*>?> {
      val components = Splitter.on(URLUtil.SCHEME_SEPARATOR).limit(2).splitToList(url)
      if (components.size != 2) {
        return ImmutableList.of<Location<*>?>()
      }
      return locator.getLocation(
        // protocol =
        components.get(0), // path =
        components.get(1),
        project,
        scope,
      )
    }
  }
}
