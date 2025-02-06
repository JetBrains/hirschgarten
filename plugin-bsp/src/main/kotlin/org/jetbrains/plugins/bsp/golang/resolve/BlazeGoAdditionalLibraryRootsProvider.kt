package org.jetbrains.plugins.bsp.golang.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import org.jetbrains.plugins.bsp.projectStructure.BspSyntheticLibrary
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import kotlin.io.path.toPath


const val GO_EXTERNAL_LIBRARY_ROOT_NAME = "Go Libraries"

class BlazeGoAdditionalLibraryRootsProvider : AdditionalLibraryRootsProvider() {
  override fun getAdditionalProjectLibraries(project: Project): MutableCollection<SyntheticLibrary> {
    val goFiles =
      project.temporaryTargetUtils.goLibraries.flatMap { it.goSources }.map { it.toPath().toFile() }.toSet().takeIf { it.isNotEmpty() }
        ?: return mutableListOf()
    val library = BspSyntheticLibrary(GO_EXTERNAL_LIBRARY_ROOT_NAME, goFiles)
    return mutableListOf<SyntheticLibrary>(library)
  }
}
