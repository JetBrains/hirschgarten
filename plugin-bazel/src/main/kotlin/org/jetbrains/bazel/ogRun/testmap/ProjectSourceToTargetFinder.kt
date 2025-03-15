/*
 * Copyright 2016-2024 The Bazel Authors. All rights reserved.
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

import com.google.common.collect.ImmutableSortedSet
import com.google.common.util.concurrent.Futures
import com.google.idea.blaze.base.dependencies.TargetInfo
import com.intellij.openapi.project.Project
import java.io.File
import java.util.*
import java.util.concurrent.Future
import java.util.function.Predicate

/**
 * Used to locate tests from source files for things like right-clicks.
 *
 *
 * It's essentially a map from source file -> reachable test rules.
 */
class ProjectSourceToTargetFinder : SourceToTargetFinder {
  override fun targetsForSourceFiles(
    project: Project?,
    sourceFiles: MutableSet<File?>,
    ruleType: RuleType?,
  ): Future<MutableCollection<TargetInfo?>?>? {
    if (Blaze.getProjectType(project).equals(ProjectType.QUERY_SYNC)) {
      val projectData: QuerySyncProjectData? =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData() as QuerySyncProjectData?
      if (projectData == null) {
        return Futures.immediateFuture<MutableCollection<TargetInfo?>?>(listOf<TargetInfo?>())
      }
      val targets: ImmutableSortedSet<TargetInfo?>? =
        sourceFiles
          .stream()
          .map<Any?> { file: File? -> projectData.getWorkspacePathResolver().getWorkspacePath(file) }
          .filter { obj: Any? -> Objects.nonNull(obj) }
          .flatMap<Any?> { path: Any? -> projectData.getReverseDeps(path.asPath()).stream() }
          .filter(Predicate.not<Any?>(Predicate { target: Any? -> target.tags().contains("no-ide") }))
          .filter { buildTarget: Any? ->
            if (ruleType.isEmpty()) {
              return@filter true
            }
            val kind: Kind? = Kind.fromRuleName(buildTarget.kind())
            if (kind == null) {
              return@filter false
            }
            kind.getRuleType().equals(ruleType.get())
          }.map<Any?>(TargetInfo::fromBuildTarget)
          .collect(ImmutableSortedSet.toImmutableSortedSet<TargetInfo>(TargetInfoComparator()))
      return Futures.immediateFuture<MutableCollection<TargetInfo?>?>(targets)
    }
    val targetMap: FilteredTargetMap? =
      SyncCache
        .getInstance(project)
        .get(ProjectSourceToTargetFinder::class.java, ProjectSourceToTargetFinder::computeTargetMap)
    if (targetMap == null) {
      return Futures.immediateFuture<MutableCollection<TargetInfo?>?>(listOf<TargetInfo?>())
    }
    val targets: ImmutableSortedSet<TargetInfo?>? =
      targetMap
        .targetsForSourceFiles(sourceFiles)
        .stream()
        .map<Any?>(TargetIdeInfo::toTargetInfo)
        .filter { target: Any? -> ruleType.isEmpty() || target.getRuleType().equals(ruleType.get()) }
        .distinct()
        .sorted(TargetInfoComparator())
        .collect(ImmutableSortedSet.toImmutableSortedSet<TargetInfo>(TargetInfoComparator()))
    return Futures.immediateFuture<MutableCollection<TargetInfo?>?>(targets)
  }

  companion object {
    private fun computeTargetMap(project: Project?, projectData: BlazeProjectData): FilteredTargetMap =
      computeTargetMap(
        project,
        projectData.getArtifactLocationDecoder(),
        projectData.getTargetMap(),
      )

    private fun computeTargetMap(
      project: Project?,
      decoder: ArtifactLocationDecoder?,
      targetMap: TargetMap,
    ): FilteredTargetMap = FilteredTargetMap(project, decoder, targetMap, Predicate { t: TargetIdeInfo? -> true })
  }
}
