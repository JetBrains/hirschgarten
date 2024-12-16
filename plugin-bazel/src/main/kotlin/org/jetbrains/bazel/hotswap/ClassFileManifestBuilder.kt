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

import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import com.intellij.debugger.impl.HotSwapProgress
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.flow.sync.query
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.runnerAction.LocalJvmRunnerAction
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

/** Used to associate data with an [ExecutionEnvironment].  */
val MANIFEST_KEY: Key<AtomicReference<ClassFileManifest>> =
  Key.create<AtomicReference<ClassFileManifest>>("bazel.debug.class.manifest")

/** Builds a .class file manifest to support hotswapping.  */
object ClassFileManifestBuilder {
  fun initStateIfNotExists(env: ExecutionEnvironment) {
    if (!HotSwapUtils.canHotSwap(env)) {
      return
    }
    if (env.getCopyableUserData(MANIFEST_KEY) == null) {
      env.putCopyableUserData(
        MANIFEST_KEY,
        AtomicReference<ClassFileManifest>(),
      )
      buildManifest(env, null)
    }
  }

  /**
   * Builds a .class file manifest, then diffs against any previously calculated manifest for this
   * debugging session.
   *
   * @return null if no diff is available (either no manifest could be calculated, or no previously
   * calculated manifest is available.
   */
  fun buildManifest(env: ExecutionEnvironment, progress: HotSwapProgress?): ClassFileManifest.Diff? {
    if (!HotSwapUtils.canHotSwap(env)) {
      return null
    }
    val configuration = getConfiguration(env) ?: return null
    val project = configuration.project
    if (!project.isBazelProject) return null
    val jvmRunEnvDeferred =
      BspCoroutineService.getInstance(project).startAsync {
        project.connection.runWithServer { server, capabilities ->
          val targets = configuration.getUserData(LocalJvmRunnerAction.targetsToPreBuild)
          query("buildTarget/jvmRunEnvironment") {
            server.buildTargetJvmRunEnvironment(JvmRunEnvironmentParams(targets))
          }
        }
      }
    progress?.setCancelWorker { jvmRunEnvDeferred.cancel() }
    val result =
      try {
        runBlocking { jvmRunEnvDeferred.await() }
      } catch (_: InterruptedException) {
        jvmRunEnvDeferred.cancel()
        throw RunCanceledByUserException()
      } catch (_: CancellationException) {
        jvmRunEnvDeferred.cancel()
        throw RunCanceledByUserException()
      } catch (e: ExecutionException) {
        throw com.intellij.execution.ExecutionException(e)
      }
    val jars =
      result.items
        .flatMap { it.classpath }
        .distinct()
        .map { File(it.safeCastToURI()) }
    val oldManifest = env.getManifest()
    val newManifest = ClassFileManifest.build(jars, oldManifest)
    env.getCopyableUserData(MANIFEST_KEY).set(newManifest)
    return ClassFileManifest.modifiedClasses(oldManifest, newManifest)
  }

  private fun getConfiguration(env: ExecutionEnvironment): ApplicationConfiguration? = env.runProfile as? ApplicationConfiguration
}

fun ExecutionEnvironment.getManifest(): ClassFileManifest? = getCopyableUserData(MANIFEST_KEY).get()
