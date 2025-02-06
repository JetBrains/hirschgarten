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
package org.jetbrains.plugins.bsp.golang.sync


import com.google.idea.blaze.base.ideinfo.ArtifactLocation

/** Declare that go files should be prefetched.  */
class GoPrefetchFileSource : PrefetchFileSource, OutputsProvider {
  public override fun isActive(languageSettings: WorkspaceLanguageSettings): Boolean {
    return languageSettings.isLanguageActive(LanguageClass.GO)
  }

  public override fun selectAllRelevantOutputs(target: TargetIdeInfo): MutableCollection<ArtifactLocation?>? {
    return if (target.getGoIdeInfo() != null) target.getGoIdeInfo()
      .getSources() else com.google.common.collect.ImmutableList.of<ArtifactLocation?>()
  }

  public override fun addFilesToPrefetch(
    project: com.intellij.openapi.project.Project?,
    projectViewSet: ProjectViewSet?,
    importRoots: ImportRoots?,
    blazeProjectData: BlazeProjectData?,
    files: MutableSet<java.io.File?>
  ) {
    files.addAll(
        BlazeGoAdditionalLibraryRootsProvider.getLibraryFiles(
            project, blazeProjectData, importRoots,
        ),
    )
  }

  public override fun prefetchFileExtensions(): MutableSet<String?> {
    return com.google.common.collect.ImmutableSet.of<String?>("go")
  }
}
