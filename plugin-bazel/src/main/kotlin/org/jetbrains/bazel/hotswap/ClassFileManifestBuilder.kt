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

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import com.intellij.debugger.impl.HotSwapProgress
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.ui.MessageCategory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.BazelHotSwapBundle
import org.jetbrains.bazel.hotswap.BazelHotSwapManager.HotSwappableDebugSession
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.flow.sync.query
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.runnerAction.LocalJvmRunnerAction
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

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
    val project = configuration.project
    if (!HotSwapUtils.canHotSwap(env, project)) {
      return null
    }
    val jvmEnvDeferred: Deferred<JvmEnvironmentResult?> =
      BspCoroutineService.getInstance(project).startAsync {
        project.connection.runWithServer { server, _ ->
          val targets = configuration.getUserData(LocalJvmRunnerAction.targetsToPreBuild)?.take(1) ?: listOf()
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
        .map { File(it.safeCastToURI()) }
        .filter { it.isFile && it.extension == "jar" }
    val oldManifest = env.getManifest()
    val newManifest = ClassFileManifest.build(jars, oldManifest)
    env.getManifestRef()?.set(newManifest)
    return ClassFileManifest.modifiedClasses(oldManifest, newManifest)
  }

  private fun BuildTargetIdentifier.isTestTarget(project: Project): Boolean =
    project.temporaryTargetUtils.targetIdToTargetInfo[this]
      ?.capabilities
      ?.canTest == true

  private suspend fun queryJvmEnvironment(
    target: BuildTargetIdentifier,
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

  private fun getConfiguration(env: ExecutionEnvironment): ApplicationConfiguration? = env.runProfile as? ApplicationConfiguration
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
