package org.jetbrains.bazel.bytecodeViewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.psi.PsiClass
import com.intellij.psi.util.ClassUtil
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.sdkcompat.bytecodeViewer.BytecodeViewerClassFileFinderCompat
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import java.nio.file.Path
import kotlin.io.path.extension

class BazelBytecodeViewerClassFileFinder : BytecodeViewerClassFileFinderCompat {
  override fun findClass(element: PsiClass, containing: PsiClass?): VirtualFile? {
    val targetElement = element.containingClass ?: element
    val project = targetElement.project
    val vFile = targetElement.containingFile.virtualFile
    val targetUtils = project.targetUtils
    val path =
      targetUtils
        .getTargetsForFile(vFile)
        .asSequence()
        .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
        .firstNotNullOfOrNull { resolveCompiledClassesPathForJVMLanguage(project, it) }
    return path?.toCompiledClassesVFSRoot(project)?.toFullClassPath(targetElement)
  }

  private fun resolveCompiledClassesPathForJVMLanguage(project: Project, target: BuildTarget): Path? {
    val binPath =
      BazelBinPathService
        .getInstance(project)
        .bazelBinPath ?: return null
    val targetDir =
      Path
        .of(binPath)
        .resolve(target.id.packagePath.toString())
    return when (target.data) {
      is JvmBuildTarget, is KotlinBuildTarget, is ScalaBuildTarget -> {
        if (target.kind.ruleType == RuleType.LIBRARY) {
          targetDir.resolve("lib${target.id.targetName}.jar")
        } else {
          targetDir.resolve("${target.id.targetName}.jar")
        }
      }

      else -> return targetDir.resolve(target.id.targetName)
    }
  }

  private fun Path.toCompiledClassesVFSRoot(project: Project): VirtualFile? {
    val isArchive = this.extension == "jar" || this.extension == "zip"
    val vfsManager = project.workspaceModel.getVirtualFileUrlManager()
    return if (isArchive) {
      this.toVirtualFileUrl(vfsManager).virtualFile?.let { JarFileSystem.getInstance().getJarRootForLocalFile(it) }
    } else {
      this.toVirtualFileUrl(vfsManager).virtualFile
    }
  }

  private fun VirtualFile.toFullClassPath(element: PsiClass): VirtualFile? {
    val jvmClassName = ClassUtil.getBinaryClassName(element) ?: return null
    return this.findChild(StringUtil.getShortName(jvmClassName) + ".class")
  }
}
