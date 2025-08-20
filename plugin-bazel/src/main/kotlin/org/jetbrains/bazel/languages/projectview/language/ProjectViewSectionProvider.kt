package org.jetbrains.bazel.languages.projectview.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.completion.DirectoriesCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import java.nio.file.Path

class LabelParser : SectionValueParser<Label> {
  override fun parse(value: String): Label = Label.parse(value)

  companion object {
    val instance = LabelParser()
  }
}

class BooleanParser : SectionValueParser<Boolean> {
  override fun parse(value: String): Boolean? = value.toBooleanStrictOrNull()

  companion object {
    val instance = BooleanParser()
  }
}

class PathParser : SectionValueParser<Path> {
  override fun parse(value: String): Path? =
    try {
      Path.of(value)
    } catch (_: Exception) {
      null
    }

  companion object {
    val instance = PathParser()
  }
}

class IncludedExcludedParser<T>(private val actualParser: SectionValueParser<T>) : SectionValueParser<Value<T>> {
  override fun parse(value: String): Value<T>? =
    if (value.startsWith("-")) {
      actualParser.parse(value.substring(1))?.let { Value.Excluded(it) }
    } else {
      actualParser.parse(value)?.let { Value.Included(it) }
    }
}

class TargetsSection : ListSection<Value<Label>>() {
  override val name = "targets"
  override val doc =
    """
    <p>A list of bazel target expressions. To resolve source files under an imported
    directory, the source must be reachable from one of your targets. Because these
    are full bazel target expressions, they support <code class="language-plaintext highlighter-rouge">/...</code> notation.</p>
    <p><strong>Note:</strong> Many IDE features rely on the information resolved from these targets.
    Without them, dependencies and generated files will not be known, appearing red
    and failing to auto-complete. To make the most of your IDE, you should ensure at
    least the files you actively work on are reachable from your targets, and
    sync to keep them resolved.</p>
    <p>Targets are built during Bazel Sync, so the more targets you have,
    the slower your Bazel Sync will be. You can use negative targets to have bazel
    ignore certain targets (eg. <code class="language-plaintext highlighter-rouge">-//java/com/google/foo:MyExpensiveTarget</code>). See
    also the <code>no-ide</code> tag.</p>
    <p>If <code >derive_targets_from_directories</code> is set,
    there’s generally no need to include targets underneath your project here,
    though you may want use this section to manually exclude some automatically
    added targets.</p>
    <p>Example:</p>
    <code>targets:
      //java/com/google/android/myproject:MyProjectDevTarget
      -//java/com/google/android/myproject:MyExpensiveTarget
      //javatests/com/google/android/myproject/...
    </code>
    """.trimIndent()
  private val parser = IncludedExcludedParser(LabelParser.instance)
  override val completionProvider = TargetCompletionProvider()

  class TargetsInstance(override val values: List<Value<Label>>) : ListInstance<Value<Label>>(values)

  override fun buildInstance(rawValues: List<String>): Instance<Value<Label>>? {
    val parsed = rawValues.mapNotNull { parser.parse(it) }
    if (parsed.any { false } || parsed.isEmpty()) {
      return null
    }
    return TargetsInstance(parsed)
  }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    if (parser.parse(element.text) == null) {
      holder.annotateError(element, "This is not a valid target expression") // TODO(solar) don't hardcode
    }
  }
}

class DirectoriesSection : ListSection<Value<Path>>() {
  override val name = "directories"
  override val doc =
    """
    <p>A list of directories to include in your project. All files in the given
    directories will be indexed (allowing you to search for them) and listed in the
    Project tool window. If a file is <em>not</em> included in your project, it will have a
    yellow tab, and you will see a warning when attempting to edit it.</p>
    <p><strong>Note:</strong> Including a directory only imports the files. If you want to resolve
    your source, it must also be reachable via a target (see below).</p>
    <p>Each directory entry must point to a workspace directory. All files under
    directories are included in the import. Directories are always recursive.</p>
    <p>If you want to <strong>exclude</strong> directories, prefix the entry with a minus (“-“)
    sign.</p>
    <p>Example:</p>
    <code>directories:
      java/com/google/android/myproject
      javatests/com/google/android/myproject
      -javatests/com/google/android/myproject/not_this
    </code>
    """.trimIndent()
  private val parser = IncludedExcludedParser(PathParser.instance)
  override val completionProvider = DirectoriesCompletionProvider()

  class DirectoriesInstance(override val values: List<Value<Path>>) : ListInstance<Value<Path>>(values)

  override fun buildInstance(rawValues: List<String>): Instance<Value<Path>>? {
    val parsed = rawValues.mapNotNull { parser.parse(it) }
    if (parsed.any { false } || parsed.isEmpty()) {
      return null
    }
    return DirectoriesInstance(parsed)
  }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    val path = parser.parse(element.text)
    if (path == null) {
      holder.annotateError(element, "This is not a valid path") // TODO(solar) don't hardcode
      return
    }
  }
}

class IndexAllFilesInAllDirectoriesSection : ScalarSection<Boolean>() {
  override val name = "index_all_files_in_all_directories"
  override val doc = null // TODO(solar)
  private val parser = BooleanParser.instance
  override val completionProvider = SimpleCompletionProvider(listOf("true", "false"))

  class IndexAllFilesInAllDirectoriesInstance(override val value: Boolean) : ScalarInstance<Boolean>(value)

  override fun buildInstance(rawValues: List<String>): Instance<Boolean>? {
    val parsed = rawValues.mapNotNull { parser.parse(it) }
    val value = parsed.singleOrNull() ?: return null
    return IndexAllFilesInAllDirectoriesInstance(value)
  }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    if (parser.parse(element.text) == null) {
      holder.annotateError(element, "This is not a valid boolean value") // TODO(solar) don't hardcode
    }
  }
}

object BuiltInProjectViewSectionProvider {
  val sections = listOf(TargetsSection(), IndexAllFilesInAllDirectoriesSection(), DirectoriesSection())
}

private fun AnnotationHolder.annotateWarning(element: PsiElement, message: String) {
  newAnnotation(HighlightSeverity.WARNING, message).range(element).create()
}

private fun AnnotationHolder.annotateError(element: PsiElement, message: String) {
  newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
}
