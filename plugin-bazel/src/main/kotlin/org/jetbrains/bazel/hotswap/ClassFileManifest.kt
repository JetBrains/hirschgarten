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

import com.intellij.execution.RunCanceledByUserException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.MultiMap
import org.jetbrains.bazel.config.BazelPluginBundle
import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.ExecutionException
import java.util.jar.JarFile

/**
 * A manifest of .class file hashes for jars needed at runtime. Used for HotSwapping.
 * The implementation assumes the source for .class file can be found exclusively from jar files.
 * */
class ClassFileManifest private constructor(
  // jar file timestamps
  private val jarFileState: Map<Path, FileTime>,
  // per-jar manifest of .class file hashes
  private val jarManifests: Map<Path, JarManifest>,
) {
  /** A per-jar map of .class files changed between manifests  */
  data class Diff(val perJarModifiedClasses: MultiMap<Path, String>)

  /** .class file manifest for a single jar.  */
  private data class JarManifest(val jar: Path, val nameToHash: Map<String, Long>) {
    companion object {
      fun build(file: Path): JarManifest? {
        try {
          val jar = JarFile(file.toFile())
          return JarManifest(
            file,
            jar
              .entries()
              .asSequence()
              .filter { entry -> entry.name.endsWith(".class") }
              .associateBy(
                keySelector = { it.name },
                valueTransform = { it.crc },
              ),
          )
        } catch (e: IOException) {
          logger.warn(
            "Error reading jar file: $file",
            e,
          )
          return null
        }
      }

      /** Returns the list of classes changed in the new manifest.  */
      fun diff(oldManifest: JarManifest?, newManifest: JarManifest): List<String> =
        newManifest
          .nameToHash
          .entries
          .filter { it.value != oldManifest?.nameToHash[it.key] }
          .map { it.key }
    }
  }

  companion object {
    private val logger: Logger =
      Logger.getInstance(ClassFileManifest::class.java)

    /** Returns a per-jar map of .class files changed in the new manifest  */
    fun modifiedClasses(oldManifest: ClassFileManifest?, newManifest: ClassFileManifest): Diff {
      val map = MultiMap<Path, String>()
      for (entry in newManifest.jarManifests.entries) {
        // quick test for object equality -- jars are often not rebuilt
        val old = oldManifest?.jarManifests[entry.key]
        if (old == entry.value) {
          continue
        }
        val changedClasses =
          JarManifest.diff(
            old,
            entry.value,
          )
        if (!changedClasses.isEmpty()) {
          map.put(entry.key, changedClasses)
        }
      }
      return Diff(map)
    }

    fun build(jars: List<Path>, previousManifest: ClassFileManifest?): ClassFileManifest {
      try {
        val diff =
          FilesDiff.diffFileTimestamps(
            previousManifest?.jarFileState,
            jars,
          )

        val jarManifests = mutableMapOf<Path, JarManifest>()
        jars.forEach { file ->
          if (!diff.updatedFiles.contains(file)) {
            previousManifest?.jarManifests?.get(file)?.let { jarManifests.put(file, it) }
          }
        }
        buildJarManifests(diff.updatedFiles)
          .forEach { m ->
            jarManifests.put(
              m.jar,
              m,
            )
          }
        return ClassFileManifest(
          diff.newFileState,
          jarManifests,
        )
      } catch (_: InterruptedException) {
        throw RunCanceledByUserException()
      } catch (e: ExecutionException) {
        throw com.intellij.execution.ExecutionException(BazelPluginBundle.message("hotswap.error.parsing.jars"), e)
      }
    }

    private fun buildJarManifests(jars: Collection<Path>): List<JarManifest> = jars.mapNotNull { jar -> JarManifest.build(jar) }
  }
}
