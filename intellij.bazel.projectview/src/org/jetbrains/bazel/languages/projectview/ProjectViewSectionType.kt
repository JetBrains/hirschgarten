package org.jetbrains.bazel.languages.projectview

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker
import org.jetbrains.bazel.languages.projectview.checker.projectViewChecker
import org.jetbrains.bazel.languages.projectview.completion.FiletypeCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.sync.environment.bazelProjectInfo
import org.jetbrains.bazel.utils.isRealPathInside
import java.nio.file.Path
import kotlin.collections.contains
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries
import kotlin.io.path.exists

@ApiStatus.Internal
interface ProjectViewSectionType<T : Any> {

  val valueChecker: ProjectViewChecker<String>?
  val completionProvider: CompletionProvider<CompletionParameters>?
  fun readFrom(values: List<String>): T?

  interface Scalar<T : Any> : ProjectViewSectionType<T> {

    fun readFrom(value: String): T?

    override fun readFrom(values: List<String>): T? = values
      .singleOrNull()
      ?.let(::readFrom)
  }

  companion object {

    val int: Scalar<Int> = scalar(String::toIntOrNull)
    val boolean: Scalar<Boolean> = variants(read = String::toBooleanStrictOrNull, variants = listOf("true", "false"))
    val label: Scalar<Label> = scalar(read = Label::parseOrNull, completionProvider = TargetCompletionProvider())
    fun string(
      completionProvider: CompletionProvider<CompletionParameters>? = null,
    ): Scalar<String> = scalar(
      read = { it },
      completionProvider = completionProvider,
    )

    fun path(
      existing: Boolean = false,
      insideProjectRoot: Boolean = false,
      completionProvider: CompletionProvider<CompletionParameters>? = null,
    ): Scalar<Path> = scalar(
      read = String::toNioPathOrNull,
      checker = pathChecker(String::toNioPathOrNull, existing, insideProjectRoot),
      completionProvider = completionProvider,
    )

    fun file(extension: String): Scalar<Path> = path(existing = true, completionProvider = FiletypeCompletionProvider(extension))

    inline fun <reified T : Enum<T>> enum(): Scalar<T> = enum(enumEntries<T>())

    fun flag(vararg commands: String): Scalar<String> = scalar(
      read = { it },
      checker = flagChecker(commands),
      completionProvider = FlagCompletionProvider(commands),
    )

    @PublishedApi
    internal fun <T : Enum<T>> enum(entries: EnumEntries<T>): Scalar<T> = variants(
      variants = entries.map { it.name.lowercase() },
      read = { value -> entries.find { it.name.equals(value, ignoreCase = true) } },
    )

    private fun <T : Any> variants(
      read: (String) -> T?,
      variants: List<String>,
    ): Scalar<T> = scalar(
      read = read,
      checker = variantsChecker(read, variants),
      completionProvider = SimpleCompletionProvider(variants),
    )

    private fun <T : Any> scalar(
      read: (String) -> T?,
      checker: ProjectViewChecker<String> = scalarChecker(read),
      completionProvider: CompletionProvider<CompletionParameters>? = null,
    ): Scalar<T> = object : Scalar<T> {

      override val valueChecker = checker
      override val completionProvider = completionProvider

      override fun readFrom(value: String): T? = read(value)
    }
  }
}

@ApiStatus.Internal
fun ProjectViewSectionType<*>.isScalar(): Boolean = this is ProjectViewSectionType.Scalar<*>

@ApiStatus.Internal
fun <T : Any> ProjectViewPsiSection.read(type: ProjectViewSectionType<T>): T? =
  type.readFrom(this.getItems().map { it.text.trim() })

@ApiStatus.Internal
fun <T : Any> ProjectViewSectionType.Scalar<T>.excludable(): ProjectViewSectionType.Scalar<ExcludableValue<T>> {
  val type = this
  return object : ProjectViewSectionType.Scalar<ExcludableValue<T>> {
    override val valueChecker = ProjectViewChecker<String> { project, value ->
      type.valueChecker?.check(project, value.removePrefix("-")).orEmpty()
    }

    override val completionProvider: CompletionProvider<CompletionParameters>?
      get() = type.completionProvider

    override fun readFrom(value: String): ExcludableValue<T>? = when {
      value.startsWith("-") -> type.readFrom(value.removePrefix("-"))?.let(ExcludableValue.Companion::excluded)
      else -> type.readFrom(value)?.let(ExcludableValue.Companion::included)
    }
  }
}

@ApiStatus.Internal
fun <T : Any> ProjectViewSectionType.Scalar<T>.list(): ProjectViewSectionType<List<T>> {
  val itemType = this
  return object : ProjectViewSectionType<List<T>> {

    override val completionProvider: CompletionProvider<CompletionParameters>? = itemType.completionProvider

    override val valueChecker: ProjectViewChecker<String>? = itemType.valueChecker

    override fun readFrom(values: List<String>) = values.mapNotNull { itemType.readFrom(it) }
  }
}

private fun <T> scalarChecker(read: (String) -> T?) = projectViewChecker<String> { _, subject ->
  if (read(subject) == null) {
    val problem = ProjectViewProblem(
      id = ProjectViewProblem.Id.InvalidValue,
      message = BazelProjectViewBundle.message("annotator.cannot.parse.value", subject),
      severity = ProjectViewProblem.Severity.Error,
    )
    yield(problem)
  }
}

private fun pathChecker(read: (String) -> Path?, existing: Boolean, insideProjectRoot: Boolean) = projectViewChecker<String> { project, subject ->
  val path = read(subject)
  if (path == null) {
    val problem = ProjectViewProblem(
      id = ProjectViewProblem.Id.InvalidValue,
      message = BazelProjectViewBundle.message("annotator.invalid.path.error", subject),
      severity = ProjectViewProblem.Severity.Error,
    )
    yield(problem)
    return@projectViewChecker
  }

  if (insideProjectRoot) {
    val rootDir = project.bazelProjectInfo.projectRootDir?.toNioPathOrNull()
    val resolvedPath = rootDir?.resolve(path)?.normalize()
    if (resolvedPath != null && !resolvedPath.isRealPathInside(rootDir)) {
      val problem = ProjectViewProblem(
        id = ProjectViewProblem.Id.InvalidValue,
        message = BazelProjectViewBundle.message("annotator.path.outside.of.project.root.error", resolvedPath, rootDir),
        severity = ProjectViewProblem.Severity.Error,
      )
      yield(problem)
      return@projectViewChecker
    }
  }

  if (!existing || !TrustedProjects.isProjectTrusted(project)) return@projectViewChecker

  val resolvedPath = when {
    path.isAbsolute -> path
    else -> project.bazelProjectInfo.projectRootDir?.toNioPath()?.resolve(path) ?: return@projectViewChecker
  }
  if (!resolvedPath.exists()) {
    val problem = ProjectViewProblem(
      id = ProjectViewProblem.Id.MissingPath,
      message = BazelProjectViewBundle.message("annotator.path.not.exists.error", resolvedPath.toString()),
      severity = ProjectViewProblem.Severity.Warning,
    )
    yield(problem)
  }
}

private fun <T> variantsChecker(read: (String) -> T?, variants: List<String>) = projectViewChecker<String> { _, subject ->
  if (read(subject) == null) {
    val problem = ProjectViewProblem(
      id = ProjectViewProblem.Id.InvalidValue,
      message = BazelProjectViewBundle.message("annotator.unknown.variant.error", subject, variants.joinToString()),
      severity = ProjectViewProblem.Severity.Error,
    )
    yield(problem)
  }
}

private fun flagChecker(commands: Array<out String>) = projectViewChecker<String> { _, subject ->
  val flag = Flag.byName(subject.takeWhile { it != '=' })
  if (flag == null) {
    val problem = ProjectViewProblem(
      id = ProjectViewProblem.Id.UnknownFlag,
      message = BazelProjectViewBundle.message("annotator.unknown.flag.error", subject),
      severity = ProjectViewProblem.Severity.Warning,
    )
    yield(problem)
    return@projectViewChecker
  }
  if (commands.any { it !in flag.option.commands }) {
    val problem = ProjectViewProblem(
      id = ProjectViewProblem.Id.FlagNotAllowed,
      message = BazelProjectViewBundle.message("annotator.flag.not.allowed.here.error", subject, commands.joinToString(", ") { "'$it'" }),
      severity = ProjectViewProblem.Severity.Warning,
    )
    yield(problem)
  }
}
