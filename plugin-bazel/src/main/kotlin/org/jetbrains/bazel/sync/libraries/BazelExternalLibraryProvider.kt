package org.jetbrains.bazel.sync.libraries

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import java.nio.file.Path

/**
 * [AdditionalLibraryRootsProvider] that needs to be handled by [ExternalLibraryManager].
 */
abstract class BazelExternalLibraryProvider : AdditionalLibraryRootsProvider() {
  abstract val libraryName: String

  abstract fun getLibraryFiles(project: Project): List<Path>

  override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
    val externalLibraryManager = ExternalLibraryManager.getInstance(project)
    val library = externalLibraryManager.getLibraryBlocking(javaClass)
    return if (library != null) listOf(library) else listOf()
  }
}
