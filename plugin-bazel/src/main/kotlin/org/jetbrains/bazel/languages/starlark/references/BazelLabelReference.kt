package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PlatformIcons
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.bazel.commons.constants.Constants.BUILD_FILE_NAMES
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.target.targetUtils

// Tested in ExternalRepoResolveTest
class BazelLabelReference(element: StarlarkStringLiteralExpression, soft: Boolean) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), soft) {
  override fun resolve(): PsiElement? = resolve(acceptOnlyFileTarget = false)

  /**
   * @see resolveLabel
   */
  fun resolve(acceptOnlyFileTarget: Boolean): PsiElement? {
    val project = element.project
    if (!project.isBazelProject || isInNameArgument()) return null
    val label = Label.parseOrNull(element.getStringContents()) ?: return null

    return resolveLabel(project, label, element.containingFile.originalFile.virtualFile, acceptOnlyFileTarget)
  }

  override fun getVariants(): Array<LookupElement> {
    if (!element.project.isBazelProject || isInNameArgument()) return emptyArray()

    if (isFileCompletionLocation()) return fileCompletion()
    if (isTargetCompletionLocation()) return targetCompletion()
    if (isLoadFilenameCompletionLocation()) return loadFilenameCompletion()
    return emptyArray()
  }

  // Checks whether it is the value of "src", "srcs" or "hdrs".
  private fun isFileCompletionLocation(): Boolean {
    if (element.parent is StarlarkListLiteralExpression) {
      val parentName = (element.parent.parent as? StarlarkNamedArgumentExpression)?.name
      return parentName in listOf("srcs", "hdrs")
    }
    return ((element.parent as? StarlarkNamedArgumentExpression)?.name == "src")
  }

  // Returns an array of all files (excluding BUILD ones) from the current directory and its
  // subdirectories, excluding subpackages — that is, subdirectories that contain a BUILD file.
  private fun fileCompletion(): Array<LookupElement> {
    val currentDirectory = element.containingFile.originalFile.virtualFile.parent
    val allFiles = mutableListOf<VirtualFile>()
    val projectFileIndex = ProjectFileIndex.getInstance(element.project)
    searchForAllFiles(projectFileIndex, currentDirectory, allFiles)

    // `VfsUtilCore.getRelativePath` can return null in the following two cases:
    // 1. The two arguments (file and ancestor) belong to different file systems.
    // 2. The second argument is not an ancestor of the first one.
    // However, since the first argument is created by traversing down the directory
    // tree starting from the second one, neither of these conditions should occur.
    val lookupElements =
      allFiles
        .map {
          fileLookupElement(VfsUtilCore.getRelativePath(it, currentDirectory)!!)
        }.toTypedArray()
    return lookupElements
  }

  private fun searchForAllFiles(
    index: ProjectFileIndex,
    currentDirectory: VirtualFile,
    allFiles: MutableList<VirtualFile>,
  ) {
    if (index.isExcluded(currentDirectory)) return
    val children = currentDirectory.children
    for (child in children) {
      if (child.isFile && !child.isBazelFile()) {
        allFiles.add(child)
      } else if (child.isDirectory) {
        if (findBuildFile(child) == null) {
          searchForAllFiles(index, child, allFiles)
        }
      }
    }
  }

  private fun fileLookupElement(name: String): LookupElement =
    LookupElementBuilder
      .create("\"" + name + "\"")
      .withIcon(PlatformIcons.FILE_ICON)
      .withPresentableText(name)

  private fun VirtualFile.isBazelFile(): Boolean = BUILD_FILE_NAMES.any { name == it }

  private fun isTargetCompletionLocation(): Boolean { // TODO: Correct target completion location validation.
    val parent = element.parent ?: return false
    return parent is StarlarkListLiteralExpression
  }

  private fun targetCompletion(): Array<LookupElement> {
    val project = element.project
    val targetUtils = project.targetUtils
    return targetUtils
      .allTargetsAndLibrariesLabels
      .map { targetLookupElement(it) }
      .toTypedArray()
  }

  private fun targetLookupElement(name: String): LookupElement =
    LookupElementBuilder
      .create("\"" + name + "\"")
      .withIcon(PlatformIcons.PACKAGE_ICON)
      .withPresentableText(name)

  private fun isInNameArgument(): Boolean {
    val parent = element.parent ?: return false
    return parent is StarlarkNamedArgumentExpression && parent.isNameArgument()
  }

  private fun isLoadFilenameCompletionLocation(): Boolean = element.parent is StarlarkFilenameLoadValue

  private object BzlFileCache {
    private class BzlFileCacheInstance(val project: Project) {
      private var cache: List<String> = listOf()
      private var cacheOld: Boolean = true
      private var listener: MessageBusConnection? = null

      private fun init() {
        listener =
          project.messageBus.connect().apply {
            subscribe(
              FileTypeIndex.INDEX_CHANGE_TOPIC,
              FileTypeIndex.IndexChangeListener { fileType ->
                cacheOld = fileType is StarlarkFileType || cacheOld
              },
            )

            subscribe(
              ProjectManager.TOPIC,
              object : ProjectManagerListener {
                override fun projectClosing(project: Project) {
                  listener?.disconnect()
                  listener = null
                }
              },
            )
          }
      }

      private fun updateCache() {
        val starlarkFiles = FileTypeIndex.getFiles(StarlarkFileType, GlobalSearchScope.allScope(project))
        val bzlFiles = starlarkFiles.filter { it.name.endsWith(".bzl") }
        val relativePaths =
          bzlFiles.mapNotNull { file ->
            VfsUtilCore.getRelativePath(file, project.rootDir)
          }
        cache = relativePaths
        cacheOld = false
      }

      fun get(): List<String> {
        if (listener == null) {
          init()
        }
        if (cacheOld) {
          updateCache()
        }

        return cache
      }
    }

    private val instances = mutableMapOf<Project, BzlFileCacheInstance>()

    fun get(project: Project): List<String> = instances.getOrPut(project) { BzlFileCacheInstance(project) }.get()
  }

  private fun filePathToLabel(relativeFilePath: String): String {
    val lastSlash = relativeFilePath.lastIndexOf('/')
    if (lastSlash == -1) {
      return "//:$relativeFilePath"
    }
    return "//" + relativeFilePath.substring(0, lastSlash) + ":" + relativeFilePath.substring(lastSlash + 1)
  }

  private fun loadFilenameCompletion(): Array<LookupElement> {
    val relativePaths = BzlFileCache.get(element.project)
    val lookupElements =
      relativePaths.map { file ->
        fileLookupElement(filePathToLabel(file))
      }
    return lookupElements.toTypedArray()
  }

  companion object {
  }
}
