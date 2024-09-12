package org.jetbrains.bazel.flow.modify

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModifier
import com.intellij.openapi.roots.libraries.Library
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.bazel.ui.widgets.findBuildFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils

class BazelProjectModelModifier : JavaProjectModelModifier() {
  override fun addModuleDependency(
    from: Module,
    to: Module,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? {
    from.jumpToBuildFile()
    return resolvedPromise<Void>()
  }

  override fun addLibraryDependency(
    from: Module,
    library: Library,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? {
    from.jumpToBuildFile()
    return resolvedPromise<Void>()
  }

  override fun addExternalLibraryDependency(
    modules: Collection<Module>,
    descriptor: ExternalLibraryDescriptor,
    scope: DependencyScope,
  ): Promise<Void>? {
    modules.firstOrNull()?.jumpToBuildFile()
    return resolvedPromise<Void>()
  }

  override fun changeLanguageLevel(module: Module, level: LanguageLevel): Promise<Void>? {
    module.jumpToBuildFile()
    return resolvedPromise<Void>()
  }

  private fun Module.jumpToBuildFile() {
    val buildTargetInfo = project.temporaryTargetUtils.getBuildTargetInfoForModule(this) ?: return
    val buildFile = findBuildFile(project, buildTargetInfo) ?: return
    EditorHelper.openInEditor(buildFile, true, true)
  }
}
