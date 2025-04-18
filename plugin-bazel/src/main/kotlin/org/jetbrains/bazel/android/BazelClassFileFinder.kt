package org.jetbrains.bazel.android

import com.android.tools.idea.projectsystem.ClassContent
import com.android.tools.idea.projectsystem.ClassFileFinder
import com.android.tools.idea.projectsystem.getPathFromFqcn
import com.android.tools.idea.rendering.classloading.loaders.JarManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.workspacemodel.entities.jvmBinaryJarsEntity
import kotlin.io.path.exists

public class BazelClassFileFinder(private val module: Module) : ClassFileFinder {
  private val jarManager = JarManager.getInstance(module.project)

  override fun findClassFile(fqcn: String): ClassContent? {
    var result: ClassContent? = null
    OrderEnumerator.orderEntries(module).recursively().forEachModule { module ->
      val classFile = findClassFileInModule(module, fqcn) ?: return@forEachModule true
      result = classFile
      false // Stop iteration when found
    }
    return result
  }

  private fun findClassFileInModule(module: Module, fqcn: String): ClassContent? {
    val binaryJars = module.moduleEntity?.jvmBinaryJarsEntity ?: return null

    val classFilePath = getPathFromFqcn(fqcn)

    return binaryJars.jars
      .asSequence()
      .map { it.toPath() }
      .filter { it.exists() }
      .mapNotNull { binaryJar ->
        jarManager.loadFileFromJar(binaryJar, classFilePath)?.let { classFileContent ->
          ClassContent.fromJarEntryContent(binaryJar.toFile(), classFileContent)
        }
      }.firstOrNull()
  }
}
