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
package org.jetbrains.bazel.run2

import com.google.common.collect.ImmutableSet
import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.run2.producers.RunConfigurationContext

/**
 * Used when we don't yet know all the configuration details, but want to provide a 'run/debug'
 * context action anyway.
 *
 *
 * This is necessary whenever details are expensive to calculate (e.g. involve searching for a
 * blaze target, or resolving PSI elements), because run configurations are set up on the EDT.
 */
interface PendingRunConfigurationContext : RunConfigurationContext {
  /** Used to indicate that a pending run configuration couldn't be successfully set up.  */
  class NoRunConfigurationFoundException(s: String?) : ExecutionException(s)

  /**
   * A result from a [PendingRunConfigurationContext], indicating that no run configuration
   * was found for this context.
   */
  class FailedPendingRunConfiguration(private val psi: PsiElement?, @JvmField val errorMessage: String?) :
    RunConfigurationContext {
    override fun getSourceElement(): PsiElement? {
      return psi
    }

    override fun setupRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
      return false
    }

    override fun matchesRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
      return false
    }
  }

  fun supportedExecutors(): Set<ExecutorType>

  /**
   * Returns true if this is an asynchronous [PendingRunConfigurationContext] that had been
   * resolved in the background. A [PendingRunConfigurationContext] that requires user action
   * will always return false until [.resolve]d.
   */
  val isDone: Boolean

  /**
   * Finish resolving the [PendingRunConfigurationContext]. Called when the user actually
   * tries to run the configuration. Block with a progress message if necessary.
   *
   * @param config will be updated if [PendingRunConfigurationContext] is resolved
   * successfully.
   * @param rerun will be called after resolving is finished to continue running the real
   * configuration.
   */
  @Throws(ExecutionException::class)
  fun resolve(env: ExecutionEnvironment, config: BlazeCommandRunConfiguration, rerun: Runnable?)
}
