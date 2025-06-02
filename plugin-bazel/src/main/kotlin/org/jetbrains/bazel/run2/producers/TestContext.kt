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
package org.jetbrains.bazel.run2.producers

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.BlazeConfigurationNameBuilder
import org.jetbrains.bazel.run2.ExecutorType
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState
import org.jetbrains.bazel.run2.state.RunConfigurationFlagsState
import java.util.function.Consumer

/** A context related to a blaze test target, used to configure a run configuration.  */
abstract class TestContext internal constructor(
  /** The [PsiElement] relevant to this test context (e.g. a method, class, file, etc.).  */
  override val sourceElement: PsiElement,
  val blazeFlags: List<BlazeFlagsModification>,
  val description: String?,
) : RunConfigurationContext {
  /** Returns true if the run configuration was successfully configured.  */
  override fun setupRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
    if (!setupTarget(config)) {
      return false
    }
    val commonState =
      config.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState::class.java) ?: return false
    commonState.commandState.command = BlazeCommandName.TEST

    val flags: MutableList<String> = ArrayList(commonState.blazeFlagsState.rawFlags)
    blazeFlags.forEach(Consumer { m: BlazeFlagsModification -> m.modifyFlags(flags) })
    commonState.blazeFlagsState.rawFlags = flags

    if (description != null) {
      val nameBuilder = BlazeConfigurationNameBuilder(config)
      nameBuilder.setTargetString(description)
      config.name = nameBuilder.build()
      config.setNameChangedByUser(true) // don't revert to generated name
    } else {
      config.setGeneratedName()
    }
    return true
  }

  /** Returns true if the run configuration matches this [TestContext].  */
  override fun matchesRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
    val commonState =
      config.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState::class.java)
        ?: return false
    if (commonState.commandState.command != BlazeCommandName.TEST) {
      return false
    }
    val flagsState = commonState.blazeFlagsState
    return matchesTarget(config) &&
      blazeFlags.stream().allMatch { m: BlazeFlagsModification? -> m!!.matchesConfigState(flagsState) }
  }

  /** Returns true if the target is successfully set up.  */
  abstract fun setupTarget(config: BlazeCommandRunConfiguration): Boolean

  /** Returns true if the run configuration target matches this [TestContext].  */
  abstract fun matchesTarget(config: BlazeCommandRunConfiguration): Boolean

  internal class KnownTargetTestContext(
    val target: BspTargetInfo.TargetInfo,
    sourceElement: PsiElement,
    blazeFlags: List<BlazeFlagsModification>,
    description: String?,
  ) : TestContext(sourceElement, blazeFlags, description) {
    override fun setupTarget(config: BlazeCommandRunConfiguration): Boolean {
      config.setTargetInfo(target)
      return true
    }

    override fun matchesTarget(config: BlazeCommandRunConfiguration): Boolean = target.label.equals(config.singleTarget)
  }

  /**
   * A modification to the blaze flags list for a run configuration. For example, setting a test
   * filter.
   */
  interface BlazeFlagsModification {
    fun modifyFlags(flags: MutableList<String>)

    fun matchesConfigState(state: RunConfigurationFlagsState): Boolean

    companion object {
      fun addFlagIfNotPresent(flag: String): BlazeFlagsModification =
        object : BlazeFlagsModification {
          override fun modifyFlags(flags: MutableList<String>) {
            if (!flags.contains(flag)) {
              flags.add(flag)
            }
          }

          override fun matchesConfigState(state: RunConfigurationFlagsState): Boolean = state.rawFlags.contains(flag)
        }

      fun testFilter(filter: String?): BlazeFlagsModification =
        object : BlazeFlagsModification {
          override fun modifyFlags(flags: MutableList<String>) {
            // remove old test filter flag if present
            flags.removeIf { it.startsWith(BlazeFlags.TEST_FILTER) }
            if (filter != null) {
              flags.add(BlazeFlags.TEST_FILTER + "=" + BlazeParametersListUtil.encodeParam(filter))
            }
          }

          override fun matchesConfigState(state: RunConfigurationFlagsState): Boolean =
            state
              .rawFlags
              .contains(BlazeFlags.TEST_FILTER + "=" + BlazeParametersListUtil.encodeParam(filter))
        }

      fun testEnv(testEnv: String?): BlazeFlagsModification =
        object : BlazeFlagsModification {
          override fun modifyFlags(flags: MutableList<String>) {
            if (testEnv != null) {
              flags.add(BlazeFlags.TEST_ENV + "=" + BlazeParametersListUtil.encodeParam(testEnv))
            }
          }

          override fun matchesConfigState(state: RunConfigurationFlagsState): Boolean =
            state
              .rawFlags
              .contains(BlazeFlags.TEST_ENV + "=" + BlazeParametersListUtil.encodeParam(testEnv))
        }
    }
  }

  /** Builder class for [TestContext].  */
  class Builder private constructor(private val sourceElement: PsiElement, private val supportedExecutors: ImmutableSet<ExecutorType>) {
    private var contextFuture: ListenableFuture<RunConfigurationContext>? = null
    private var targetFuture: ListenableFuture<BspTargetInfo.TargetInfo>? = null
    private val blazeFlags: ImmutableList.Builder<BlazeFlagsModification> =
      ImmutableList.builder()
    private var description: String? = null

    @CanIgnoreReturnValue
    fun setContextFuture(contextFuture: ListenableFuture<RunConfigurationContext>?): Builder {
      this.contextFuture = contextFuture
      return this
    }

    @CanIgnoreReturnValue
    fun setTarget(future: ListenableFuture<BspTargetInfo.TargetInfo>?): Builder {
      this.targetFuture = future
      return this
    }

    @CanIgnoreReturnValue
    fun setTarget(target: BspTargetInfo.TargetInfo): Builder {
      this.targetFuture = Futures.immediateFuture(target)
      return this
    }

    @CanIgnoreReturnValue
    fun setTestFilter(filter: String?): Builder {
      if (filter != null) {
        blazeFlags.add(BlazeFlagsModification.Companion.testFilter(filter))
      }
      return this
    }

    @CanIgnoreReturnValue
    fun addBlazeFlagsModification(modification: BlazeFlagsModification): Builder {
      this.blazeFlags.add(modification)
      return this
    }

    @CanIgnoreReturnValue
    fun setDescription(description: String?): Builder {
      this.description = description
      return this
    }

    fun addTestEnv(env: String?): Builder {
      this.blazeFlags.add(BlazeFlagsModification.Companion.testEnv(env))
      return this
    }

    fun build(): TestContext {
      if (contextFuture != null) {
        Preconditions.checkState(targetFuture == null)
        return PendingAsyncTestContext(
          supportedExecutors,
          contextFuture,
          "Resolving test context",
          sourceElement,
          blazeFlags.build(),
          description,
        )
      }
      Preconditions.checkState(targetFuture != null)
      return PendingAsyncTestContext.fromTargetFuture(
        supportedExecutors,
        targetFuture,
        sourceElement,
        blazeFlags.build(),
        description,
      )
    }
  }

  companion object {
    fun builder(sourceElement: PsiElement, supportedExecutors: ImmutableSet<ExecutorType>): Builder =
      TestContext.Builder(sourceElement, supportedExecutors)
  }
}
