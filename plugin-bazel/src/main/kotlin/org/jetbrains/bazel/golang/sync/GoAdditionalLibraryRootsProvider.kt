package org.jetbrains.bazel.golang.sync

import com.google.common.collect.ImmutableList.toImmutableList
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.golang.resolve.BazelGoPackage
import org.jetbrains.bazel.sync.libraries.BazelExternalLibraryProvider
import java.nio.file.Path
import java.util.function.Predicate
import kotlin.io.path.extension

const val GO_EXTERNAL_LIBRARY_ROOT_NAME = "Go Libraries (from Bazel plugin)"

class GoAdditionalLibraryRootsProvider : BazelExternalLibraryProvider() {
  override val libraryName: String = GO_EXTERNAL_LIBRARY_ROOT_NAME

  override fun getLibraryFiles(project: Project): List<Path> {
    if (!project.isBazelProject) return emptyList()
    val workspacePath = project.rootDir.toNioPath()
    val isExternal = Predicate<Path> { !it.startsWith(workspacePath) }

    // don't use sync cache, because
    // 1. this is used during sync before project data is saved
    // 2. the roots provider is its own cache
    return BazelGoPackage
      .getUncachedTargetToFileMap(project)
      .values()
      .stream()
      .filter(isExternal)
      .filter { it.extension == "go" }
      .distinct()
      .collect(toImmutableList())
  }
}
