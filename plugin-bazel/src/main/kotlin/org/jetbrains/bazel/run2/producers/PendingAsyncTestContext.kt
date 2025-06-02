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
package org.jetbrains.bazel.run2.producers

import com.google.common.base.Function
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.util.ui.UIUtil
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.ExecutorType
import org.jetbrains.bazel.run2.PendingRunConfigurationContext
import org.jetbrains.bazel.run2.PendingRunConfigurationContext.FailedPendingRunConfiguration
import org.jetbrains.bazel.run2.PendingRunConfigurationContext.NoRunConfigurationFoundException

/**
 * For situations where we appear to be in a recognized test context, but can't efficiently resolve
 * the psi elements and/or relevant blaze target.
 *
 *
 * A [BlazeCommandRunConfiguration] will be produced synchronously, then filled in later
 * when the full context is known.
 */
internal class PendingAsyncTestContext(
  private val supportedExecutors: Set<ExecutorType>,
  future: ListenableFuture<RunConfigurationContext>,
  private val progressMessage: String?,
  sourceElement: PsiElement,
  blazeFlags: List<BlazeFlagsModification>,
  description: String?,
) : TestContext(sourceElement, blazeFlags, description),
  PendingRunConfigurationContext {
  private val future: ListenableFuture<RunConfigurationContext> = recursivelyResolveContext(future)

  override fun supportedExecutors(): Set<ExecutorType> = supportedExecutors

  override val isDone: Boolean
    get() = future.isDone

  @Throws(ExecutionException::class)
  override fun resolve(
    env: ExecutionEnvironment,
    config: BlazeCommandRunConfiguration,
    rerun: Runnable,
  ) {
    waitForFutureUnderProgressDialog(env.project)
    rerun.run()
  }

  override fun setupTarget(config: BlazeCommandRunConfiguration): Boolean {
    config.setPendingContext(this)
    if (future.isDone) {
      // set it up synchronously, and return the result
      return doSetupPendingContext(config)
    } else {
      future.addListener({ doSetupPendingContext(config) }, MoreExecutors.directExecutor())
      return true
    }
  }

  private fun doSetupPendingContext(config: BlazeCommandRunConfiguration): Boolean {
    try {
      val context = this.futureHandlingErrors
      val success = context.setupRunConfiguration(config)
      if (success) {
        if (config.getPendingContext() === this) {
          // remove this pending context from the config since it is done
          // however, if context became the new pending context, leave it alone
          config.clearPendingContext()
        }
        return true
      }
    } catch (e: RunCanceledByUserException) {
      // silently ignore
    } catch (e: NoRunConfigurationFoundException) {
    } catch (e: ExecutionException) {
      logger.warn(e)
    }
    return false
  }

  override fun matchesRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
    if (!future.isDone) {
      return super.matchesRunConfiguration(config)
    }
    try {
      val context = future.get()
      return context.matchesRunConfiguration(config)
    } catch (e: java.util.concurrent.ExecutionException) {
      return false
    } catch (e: InterruptedException) {
      return false
    }
  }

  override fun matchesTarget(config: BlazeCommandRunConfiguration): Boolean = sourceElementString == config.contextElementString

  /**
   * Waits for the run configuration to be configured, displaying a progress dialog if necessary.
   *
   * @throws ExecutionException if the run configuration is not successfully
   * configured
   */
  @Throws(ExecutionException::class)
  private fun waitForFutureUnderProgressDialog(project: Project?) {
    if (future.isDone()) {
      this.futureHandlingErrors
    }
    // The progress indicator must be created on the UI thread.
    val indicator: ProgressWindow =
      UIUtil.invokeAndWaitIfNeeded<BackgroundableProcessIndicator>(
        Computable {
          BackgroundableProcessIndicator(
            project,
            progressMessage,
            PerformInBackgroundOption.ALWAYS_BACKGROUND,
            "Cancel",
            "Cancel", // cancellable=
            true,
          )
        },
      )

    indicator.setIndeterminate(true)
    indicator.start()
    indicator.addStateDelegate(
      object : AbstractProgressIndicatorExBase() {
        override fun cancel() {
          super.cancel()
          future.cancel(true)
        }
      },
    )
    try {
      this.futureHandlingErrors
    } finally {
      if (indicator.isRunning) {
        indicator.stop()
        indicator.processFinish()
      }
    }
  }

  @get:Throws(ExecutionException::class)
  private val futureHandlingErrors: RunConfigurationContext
    get() {
      try {
        val result = future.get() ?: throw NoRunConfigurationFoundException("Run configuration setup failed.")
        if (result is FailedPendingRunConfiguration) {
          throw NoRunConfigurationFoundException(
            result.errorMessage,
          )
        }
        return result
      } catch (e: InterruptedException) {
        throw RunCanceledByUserException()
      } catch (e: java.util.concurrent.ExecutionException) {
        throw ExecutionException(e)
      }
    }

  companion object {
    private val logger = Logger.getInstance(PendingAsyncTestContext::class.java)

    fun fromTargetFuture(
      supportedExecutors: ImmutableSet<ExecutorType>,
      target: ListenableFuture<TargetInfo?>,
      sourceElement: PsiElement,
      blazeFlags: List<BlazeFlagsModification>,
      description: String?,
    ): PendingAsyncTestContext {
      val project = sourceElement.project
      val buildSystem = "Bazel"
      val progressMessage: String = String.format("Searching for %s target", buildSystem)
      val future =
        Futures.transform<TargetInfo, RunConfigurationContext>(
          target,
          Function { t: TargetInfo? ->
            if (t == null) {
              return@Function FailedPendingRunConfiguration(
                sourceElement,
                String.format("No %s target found.", buildSystem),
              )
            }
            val context =
              PendingWebTestContext.findWebTestContext(
                project,
                supportedExecutors,
                t,
                sourceElement,
                blazeFlags,
                description,
              )
            context ?: KnownTargetTestContext(t, sourceElement, blazeFlags, description)
          },
          MoreExecutors.directExecutor(),
        )
      return PendingAsyncTestContext(
        supportedExecutors,
        future,
        progressMessage,
        sourceElement,
        blazeFlags,
        description,
      )
    }

    /**
     * Returns a future with all currently-unknown details of this configuration context resolved.
     *
     *
     * Handles the case where there are nested [PendingAsyncTestContext]s.
     */
    private fun recursivelyResolveContext(future: ListenableFuture<RunConfigurationContext>): ListenableFuture<RunConfigurationContext> =
      Futures.transformAsync(
        future,
        AsyncFunction { c: RunConfigurationContext ->
          if (c is PendingAsyncTestContext) {
            recursivelyResolveContext(c.future)
          } else {
            Futures.immediateFuture(c)
          }
        },
        MoreExecutors.directExecutor(),
      )
  }
}
