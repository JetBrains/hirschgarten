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
package org.jetbrains.bazel.hotswap

import com.intellij.debugger.impl.HotSwapProgress
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.ui.MessageCategory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelHotSwapBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.hotswap.BazelHotSwapManager.HotSwappableDebugSession
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.run.config.HotswappableRunConfiguration
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import java.nio.file.Files
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.extension

/** Used to associate data with an [ExecutionEnvironment].  */
private val MANIFEST_KEY: Key<AtomicReference<ClassFileManifest>> =
  Key.create<AtomicReference<ClassFileManifest>>("bazel.debug.class.manifest")

/** Builds a .class file manifest to support hotswapping.  */
object ClassFileManifestBuilder {
  fun initStateIfNotExists(session: HotSwappableDebugSession, project: Project) {
    val env = session.env
    if (!HotSwapUtils.canHotSwap(env, project)) {
      return
    }
    if (env.getManifestRef() == null) {
      env.putCopyableUserData(
        MANIFEST_KEY,
        AtomicReference<ClassFileManifest>(),
      )
      buildManifest(session, null)
    }
  }

  /**
   * Builds a .class file manifest, then diffs against any previously calculated manifest for this
   * debugging session.
   *
   * @return null if no diff is available (either no manifest could be calculated, or no previously
   * calculated manifest is available.
   */
  fun buildManifest(session: HotSwappableDebugSession, progress: HotSwapProgress?): ClassFileManifest.Diff? {
    val env = session.env
    val configuration = getConfiguration(env) ?: return null
    val project = configuration.getProject()
    if (!HotSwapUtils.canHotSwap(env, project)) {
      return null
    }
    val jvmEnvDeferred: Deferred<JvmEnvironmentResult?> =
      BazelCoroutineService.getInstance(project).startAsync {
        project.connection.runWithServer { server ->
          val targets = configuration.getAffectedTargets()
          if (targets.isEmpty()) {
            progress?.addMessage(
              session.session,
              MessageCategory.WARNING,
              BazelHotSwapBundle.message("hotswap.message.manifest.empty.target.error"),
            )
            return@runWithServer null
          }
          val target = targets.first()
          val isTest = target.isTestTarget(project)
          return@runWithServer queryJvmEnvironment(target, server, isTest)
        }
      }
    progress?.setCancelWorker { jvmEnvDeferred.cancel() }
    val result =
      try {
        runBlocking { jvmEnvDeferred.await() }
      } catch (_: InterruptedException) {
        jvmEnvDeferred.cancel()
        throw RunCanceledByUserException()
      } catch (_: CancellationException) {
        jvmEnvDeferred.cancel()
        throw RunCanceledByUserException()
      } catch (e: ExecutionException) {
        throw com.intellij.execution.ExecutionException(e)
      }
    if (result == null) {
      return null
    }
    val jars =
      result
        .getItems()
        .flatMap { it.classpath }
        .distinct()
        .filter { Files.isRegularFile(it) && it.extension == "jar" }
    val oldManifest = env.getManifest()
    val newManifest = ClassFileManifest.build(jars, oldManifest)
    env.getManifestRef()?.set(newManifest)
    return ClassFileManifest.modifiedClasses(oldManifest, newManifest)
  }

  private fun CanonicalLabel.isTestTarget(project: Project): Boolean =
    project.targetUtils
      .getBuildTargetForLabel(this)
      ?.kind
      ?.ruleType == RuleType.TEST

  private suspend fun queryJvmEnvironment(
    target: CanonicalLabel,
    server: JoinedBuildServer,
    isTest: Boolean,
  ): JvmEnvironmentResult =
    if (isTest) {
      JvmEnvironmentResult.JvmTestEnv(
        query("buildTarget/jvmTestEnvironment") {
          server.buildTargetJvmTestEnvironment(JvmTestEnvironmentParams(listOf(target)))
        },
      )
    } else {
      JvmEnvironmentResult.JvmRunEnv(
        query("buildTarget/jvmRunEnvironment") {
          server.buildTargetJvmRunEnvironment(JvmRunEnvironmentParams(listOf(target)))
        },
      )
    }

  private fun ExecutionEnvironment.getManifest(): ClassFileManifest? = getManifestRef()?.get()

  private fun ExecutionEnvironment.getManifestRef(): AtomicReference<ClassFileManifest>? = getCopyableUserData(MANIFEST_KEY)

  private fun getConfiguration(env: ExecutionEnvironment): HotswappableRunConfiguration? = env.runProfile as? HotswappableRunConfiguration
}

private sealed interface JvmEnvironmentResult {
  fun getItems(): List<JvmEnvironmentItem> =
    when (this) {
      is JvmRunEnv -> result.items
      is JvmTestEnv -> result.items
    }

  data class JvmRunEnv(val result: JvmRunEnvironmentResult) : JvmEnvironmentResult

  data class JvmTestEnv(val result: JvmTestEnvironmentResult) : JvmEnvironmentResult
}
