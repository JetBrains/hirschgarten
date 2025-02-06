/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.plugins.bsp.golang.run.producers

import com.google.idea.blaze.base.settings.Blaze

/** Suppresses certain non-Blaze configuration producers in Blaze projects.  */
class NonBlazeProducerSuppressor : com.intellij.openapi.startup.StartupActivity.DumbAware {
  override fun runActivity(project: com.intellij.openapi.project.Project) {
    if (Blaze.isBlazeProject(project)) {
      com.google.idea.blaze.golang.run.producers.NonBlazeProducerSuppressor.Companion.suppressProducers(project)
    }
  }

  companion object {
    /**
     * Returns a list of run configuration producers to suppress for Blaze projects.
     *
     *
     * These classes must all be accessible from the Blaze plugin's classpath (e.g. they shouldn't
     * belong to any plugins not listed as dependencies of the Blaze plugin).
     */
    private val PRODUCERS_TO_SUPPRESS: com.google.common.collect.ImmutableList<java.lang.Class<out com.intellij.execution.actions.RunConfigurationProducer<*>?>?> =
      com.google.common.collect.ImmutableList.of<java.lang.Class<out com.intellij.execution.actions.RunConfigurationProducer<*>?>?>(
          GoApplicationRunConfigurationProducer::class.java,
          GotestRunConfigurationProducer::class.java,
          GobenchRunConfigurationProducer::class.java,
          GocheckRunConfigurationProducer::class.java,
      )

    @com.google.common.annotations.VisibleForTesting
    fun suppressProducers(project: com.intellij.openapi.project.Project) {
      val producerService: com.intellij.execution.RunConfigurationProducerService =
        com.intellij.execution.RunConfigurationProducerService.getInstance(project)
      com.google.idea.blaze.golang.run.producers.NonBlazeProducerSuppressor.Companion.PRODUCERS_TO_SUPPRESS.forEach(
          java.util.function.Consumer { ignoredProducer: java.lang.Class<out com.intellij.execution.actions.RunConfigurationProducer<*>?>? ->
              producerService.addIgnoredProducer(ignoredProducer)
          },
      )
    }
  }
}
