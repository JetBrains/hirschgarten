package org.jetbrains.bazel.flow.open

// TODO rewrite with no VirtualFile operations!
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.jetbrains.annotations.ApiStatus

private const val PROJECTVIEW_DEFAULT_TEMPLATE_PATH = "tools/intellij/.managed.bazelproject"

private val INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE =
  """
  # This project view file may be overwritten by the Bazel plugin.
  # To use it as default, place it in the Bazel module or workspace root directory.
  # For more options, see project view file documentation: https://www.jetbrains.com/help/idea/bazel-project-view.html
  
  derive_targets_from_directories: true
  directories: %s
  
  # Uncomment these lines to make indexing quicker:
  # import_depth: 0
  # import_ijars: true
  
  """.trimIndent()

@ApiStatus.Internal
object ProjectViewFileUtils {
  fun projectViewTemplate(
    projectRootDir: VirtualFile,
    format: String? = null,
    forceDefaultTemplate: Boolean = false,
  ): String {
    val rawText = if (forceDefaultTemplate) {
      INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE
    }
    else {
        projectRootDir.resolveFromRootOrRelative(PROJECTVIEW_DEFAULT_TEMPLATE_PATH)
          ?.readText()
        ?: INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE
    }
    return rawText.format(format.orEmpty().ifBlank { "." })
  }
}
