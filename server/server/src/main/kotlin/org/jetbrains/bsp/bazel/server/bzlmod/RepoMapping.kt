package org.jetbrains.bsp.bazel.server.bzlmod

import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.ModuleOutputParser
import org.jetbrains.bsp.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bsp.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.bazel.workspacecontext.externalRepositoriesTreatedAsInternal
import java.nio.file.Path
import kotlin.io.path.Path

data class RepoMapping(val moduleCanonicalNameToLocalPath: Map<Label, Path>, val moduleApparentNameToCanonicalName: Map<String, String>) {
  val externalModuleNamesTreatedAsInternal =
}

fun calculateRepoMapping(workspaceContextProvider: WorkspaceContextProvider, bazelRunner: BazelRunner): RepoMapping {
  val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
  val moduleResolver = ModuleResolver(bazelRunner, ModuleOutputParser())
  val moduleCanonicalNameToLocalPath = mutableMapOf<String, Path>()
  // empty string is the name of the root module
  val moduleApparentNameToCanonicalName = moduleResolver.getRepoMapping("") {  }
  for (externalRepo in workspaceContext.externalRepositoriesTreatedAsInternal) {
    try {
      val showRepoResult = moduleResolver.resolveModule(externalRepo) {}
      // TODO the name in the result is the canonical name, not the name we passed in
      when (showRepoResult) {
        is ShowRepoResult.LocalRepository -> moduleCanonicalNameToLocalPath[showRepoResult.name] = Path(showRepoResult.path)
        else -> {
          // TODO log
        }
      }
    } catch (e: Exception) {
      // ignore and continue because what else to do
    }
  }

  return RepoMapping(moduleCanonicalNameToLocalPath, moduleApparentNameToCanonicalName)
}
