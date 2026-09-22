package org.jetbrains.bazel.bytecodeViewer

import com.intellij.byteCodeViewer.BytecodeViewerClassFileFinder
import com.intellij.ide.util.JavaAnonymousClassesHelper
import com.intellij.ide.util.JavaLocalClassesHelper
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.target.getTargetDataForLabel
import org.jetbrains.bazel.target.targetStorage
import java.nio.file.Path
import kotlin.io.path.extension

internal class BazelBytecodeViewerClassFileFinder : BytecodeViewerClassFileFinder {
  override fun findClass(element: PsiClass, containing: PsiClass?): VirtualFile? {
    val targetElement = element.containingClass ?: element
    val project = targetElement.project
    val vFile = targetElement.containingFile.virtualFile
    val targetUtils = project.targetStorage
    return targetUtils.getTargetsForFile(vFile)
      .asSequence()
      .mapNotNull { targetUtils.getTargetDataForLabel<JvmBuildTarget>(it) }
      .flatMap { it.binaryOutputs.getOutputLocations() }
      .mapNotNull { targetUtils.resolveExecrootOutputLocation(it) }
      .firstNotNullOfOrNull { it.toCompiledClassesVFSRoot(project)?.toFullClassPath(element, containing) }
  }

  private fun Path.toCompiledClassesVFSRoot(project: Project): VirtualFile? {
    val isArchive = this.extension == "jar" || this.extension == "zip"
    val vfsManager = project.workspaceModel.getVirtualFileUrlManager()
    val file = this.toVirtualFileUrl(vfsManager).virtualFile ?: return null
    return if (isArchive) {
      JarFileSystem.getInstance().getJarRootForLocalFile(file)
    }
    else {
      file
    }
  }

  private fun VirtualFile.toFullClassPath(element: PsiClass, containing: PsiClass?): VirtualFile? {
    val jvmClassName = getBinaryClassName(containing ?: element) ?: return null
    return this.findChild(StringUtil.getShortName(jvmClassName) + ".class")
  }

  fun getBinaryClassName(aClass: PsiClass): String? {
    if (PsiUtil.isLocalOrAnonymousClass(aClass)) {
      val parentClass = PsiTreeUtil.getParentOfType(aClass, PsiClass::class.java)
      if (parentClass == null) {
        return null
      }
      val parentName = getBinaryClassName(parentClass)
      if (parentName == null) {
        return null
      }
      if (aClass is PsiAnonymousClass) {
        return parentName + JavaAnonymousClassesHelper.getName(aClass)
      }
      else {
        return parentName + JavaLocalClassesHelper.getName(aClass)
      }
    }

    return ClassUtil.getJVMClassName(aClass)
  }
}
