package org.jetbrains.bazel.projectview.parser

import org.jetbrains.bazel.projectview.model.ProjectView
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * Project view file parser. Its purpose is to parse *.bazelproject file and create an instance of
 * ProjectView.
 *
 * @see org.jetbrains.bazel.projectview.model.ProjectView
 */
interface ProjectViewParser {
  /**
   * Parses file under `projectViewFilePath`.
   *
   * @param projectViewFilePath path to a file with a project view
   * @return parsed `ProjectView` instance.
   * **Some fields in the returned `ProjectView` might be empty**
   *
   * @throws NoSuchFileException file under `projectViewFilePath` doesn't exist
   * @throws Exception any other fail happened
   */
  fun parse(projectViewFilePath: Path): ProjectView = parse(Files.readString(projectViewFilePath))

  /**
   * similar to [parse] but does not throw any exceptions.
   * this is used alongside with `try_import` statement
   */
  fun tryParse(projectViewFilePath: Path): ProjectView? =
    try {
      parse(projectViewFilePath)
    } catch (_: Exception) {
      null
    }

  /**
   * Parses `projectViewFileContent`.
   *
   * @param projectViewFileContent string with project view
   * @return
   *
   * `ProjectView` if parsing has finished with
   * success, it means:
   *
   * 1) `projectViewFileContent` was successfully parsed (not all values have to
   * be provided -- some fields in `ProjectView` might be null).
   *
   * @throws Exception if any failure happens
   */
  fun parse(projectViewFileContent: String): ProjectView

  class ImportNotFound(missingFile: Path) : NoSuchFileException(missingFile.toString())
}
