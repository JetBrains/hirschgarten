package com.intellij.bazel.python.backend

import com.intellij.ide.util.ModuleRendererFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.QualifiedName
import com.intellij.util.TextWithIcon
import com.jetbrains.python.PyNames
import com.jetbrains.python.PyPsiBundle
import com.jetbrains.python.codeInsight.typing.convertStubToRuntimePackageName
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyPossibleClassMember
import com.jetbrains.python.psi.impl.PyElementPresentation
import com.jetbrains.python.pyi.PyiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import java.nio.file.Path

@ApiStatus.Internal
class BazelPythonModuleRendererFactory : ModuleRendererFactory() {
  override fun handles(element: Any?): Boolean =
    element is PyElement && element.project.isBazelProject

  override fun rendersLocationString(): Boolean = true

  override fun getModuleTextWithIcon(element: Any?): TextWithIcon? {
    val locationString = (element as? PyElement)?.bazelPythonLocationString() ?: return null
    return TextWithIcon(locationString, null)
  }
}

internal fun PyElement.bazelPythonLocationString(): String? {
  val containingFile = containingFile ?: return null
  val packageForFile = getBazelPackageForFile(containingFile) ?: return null
  val isPyiFile = containingFile is PyiFile

  val containingClass: PyClass? = (this as? PyPossibleClassMember)?.containingClass
  if (containingClass != null) {
    return if (isPyiFile) {
      PyPsiBundle.message("element.presentation.location.string.in.class.stub", containingClass.name, packageForFile)
    }
    else {
      PyPsiBundle.message("element.presentation.location.string.in.class", containingClass.name, packageForFile)
    }
  }

  return if (isPyiFile) {
    PyPsiBundle.message("element.presentation.location.string.module.stub", packageForFile)
  }
  else {
    PyPsiBundle.message("element.presentation.location.string.module", packageForFile)
  }
}

private fun PyElement.getBazelPackageForFile(containingFile: PsiFile): String? {
  val nativePackage = PyElementPresentation.getPackageForFile(containingFile)
  if (nativePackage != null) return nativePackage

  val vFile = containingFile.virtualFile ?: return null
  val indexedPackage = findIndexedPackageForFile(vFile)
  if (indexedPackage != null) return indexedPackage

  return getProjectRootQualifiedName(vFile)
}

private fun PyElement.findIndexedPackageForFile(vFile: VirtualFile): String? {
  val path = Path.of(vFile.path)
  return project.service<PythonResolveIndexService>().findShortestQualifiedName(path)?.toString()
}

private fun PyElement.getProjectRootQualifiedName(vFile: VirtualFile): String? {
  val root = project.rootDir
  val relativePath = VfsUtilCore.getRelativePath(vFile, root, VfsUtilCore.VFS_SEPARATOR_CHAR)
  return relativePath?.toQualifiedName(vFile.isDirectory)
}

private fun String.toQualifiedName(isDirectory: Boolean): String? {
  val components = StringUtil.split(this, VfsUtilCore.VFS_SEPARATOR).toMutableList()
  if (components.isEmpty()) return null

  val lastIndex = components.lastIndex
  val nameWithoutExtension = FileUtilRt.getNameWithoutExtension(components[lastIndex])
  if (!isDirectory && nameWithoutExtension == PyNames.INIT) {
    components.removeAt(lastIndex)
  }
  else {
    components[lastIndex] = nameWithoutExtension
  }

  if (components.isEmpty() || components.any { it.contains(".") || !PyNames.isIdentifier(it) }) {
    return null
  }
  return convertStubToRuntimePackageName(QualifiedName.fromComponents(components)).toString()
}
