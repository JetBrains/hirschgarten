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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.MultiMap
import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.jar.JarFile

/**
 * A manifest of .class file hashes for jars needed at runtime. Used for HotSwapping.
 * The implementation assumes the source for .class file can be found exclusively from jar files.
 * */
class JarFileManifest private constructor(
  // jar file timestamps
  private val jarFileState: Map<Path, FileTime>,
  // per-jar manifest of .class file hashes
  private val jarManifests: Map<Path, JarManifest>,
) {
  data class Diff(val perJarModifiedFiles: MultiMap<Path, String>)

  /** file manifest for a single jar.  */
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
              .filter { entry -> !entry.isDirectory }
              .associateBy(
                keySelector = { it.name },
                valueTransform = { it.crc },
              ),
          )
        }
        catch (e: IOException) {
          logger.warn(
            "Error reading jar file: $file",
            e,
          )
          return null
        }
      }

      /** Returns the list of files changed in the new manifest. */
      fun diff(oldManifest: JarManifest?, newManifest: JarManifest): List<String> =
        newManifest
          .nameToHash
          .entries
          .filter {
            val oldHash = oldManifest?.nameToHash[it.key]
            it.value != oldHash
          }
          .map { it.key }
    }
  }

  companion object {
    private val logger: Logger =
      Logger.getInstance(JarFileManifest::class.java)

    /** Returns a per-jar map of files changed in the new manifest  */
    fun diffJarManifests(oldManifest: JarFileManifest?, newManifest: JarFileManifest): Diff {
      val changedFilesMap = MultiMap<Path, String>()
      for (entry in newManifest.jarManifests.entries) {
        // quick test for object equality -- jars are often not rebuilt
        val old = oldManifest?.jarManifests[entry.key]
        if (old == entry.value) {
          continue
        }
        val changedFiles =
          JarManifest.diff(
            old,
            entry.value,
          )
        if (!changedFiles.isEmpty()) {
          changedFilesMap.put(entry.key, changedFiles)
        }
      }
      return Diff(perJarModifiedFiles = changedFilesMap)
    }

    fun build(jars: Collection<Path>, previousManifest: JarFileManifest?): JarFileManifest {
      val diff = FilesDiff.diffFileTimestamps(previousManifest?.jarFileState, jars)
      val jarManifests = mutableMapOf<Path, JarManifest>()
      for (file in jars) {
        if (!diff.updatedFiles.contains(file)) {
          previousManifest?.jarManifests?.get(file)?.let { jarManifests[file] = it }
        }
      }
      for (m in buildJarManifests(diff.updatedFiles)) {
        jarManifests[m.jar] = m
      }
      return JarFileManifest(diff.newFileState, jarManifests)
    }

    private fun buildJarManifests(jars: Collection<Path>): List<JarManifest> = jars.mapNotNull { jar -> JarManifest.build(jar) }
  }
}
