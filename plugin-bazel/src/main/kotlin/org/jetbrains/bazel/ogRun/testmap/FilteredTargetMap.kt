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
package org.jetbrains.bazel.ogRun.testmap

import com.google.common.collect.*
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo
import com.intellij.openapi.project.Project
import java.io.File
import java.util.*
import java.util.function.Predicate

/** Filters a [TargetMap] according to a given filter.  */
class FilteredTargetMap(
  private val project: Project?,
  decoder: ArtifactLocationDecoder,
  targetMap: TargetMap,
  filter: Predicate<TargetIdeInfo?>
) {
  private val rootsMap: Multimap<File?, TargetKey?>
  private val targetMap: TargetMap
  private val filter: Predicate<TargetIdeInfo?>

  init {
    this.rootsMap = createRootsMap(decoder, targetMap.targets())
    this.targetMap = targetMap
    this.filter = filter
  }

  fun targetsForSourceFile(sourceFile: File): ImmutableSet<TargetIdeInfo?> {
    return targetsForSourceFiles(ImmutableList.of<File?>(sourceFile))
  }

  fun targetsForSourceFiles(sourceFiles: MutableCollection<File?>): ImmutableSet<TargetIdeInfo?> {
    val blazeProjectData: BlazeProjectData? =
      BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
    if (blazeProjectData != null) {
      return targetsForSourceFilesImpl(ReverseDependencyMap.get(project), sourceFiles)
    }
    return ImmutableSet.of<TargetIdeInfo?>()
  }

  private fun targetsForSourceFilesImpl(
    rdepsMap: ImmutableMultimap<TargetKey?, TargetKey?>, sourceFiles: MutableCollection<File?>
  ): ImmutableSet<TargetIdeInfo?> {
    val result: ImmutableSet.Builder<TargetIdeInfo?> = ImmutableSet.builder<TargetIdeInfo?>()
    val roots: MutableSet<TargetKey?>? =
      sourceFiles.stream()
        .flatMap<TargetKey?> { f: File? -> rootsMap.get(f).stream() }
        .collect(ImmutableSet.toImmutableSet<TargetKey?>())

    val todo: Queue<TargetKey> = Queues.newArrayDeque<TargetKey>()
    todo.addAll(roots)
    val seen: MutableSet<TargetKey?> = Sets.newHashSet<TargetKey?>()
    while (!todo.isEmpty()) {
      val targetKey: TargetKey = todo.remove()
      if (!seen.add(targetKey)) {
        continue
      }

      val target: TargetIdeInfo = targetMap.get(targetKey)
      if (filter.test(target)) {
        result.add(target)
      }
      todo.addAll(rdepsMap.get(targetKey))
    }
    return result.build()
  }

  companion object {
    private fun createRootsMap(
      decoder: ArtifactLocationDecoder, targets: MutableCollection<TargetIdeInfo>
    ): Multimap<File?, TargetKey?> {
      val result: Multimap<File?, TargetKey?> = ArrayListMultimap.create<File?, TargetKey?>()
      for (target in targets) {
        target.getSources().stream()
          .map(decoder::resolveSource)
          .filter({ obj: Any? -> Objects.nonNull(obj) })
          .forEach({ f -> result.put(f, target.getKey()) })
      }
      return result
    }
  }
}
