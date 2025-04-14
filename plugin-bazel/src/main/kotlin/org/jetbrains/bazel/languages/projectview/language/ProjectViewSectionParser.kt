package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection.Scalar

sealed interface ProjectViewSectionParser {
  fun parse(items: List<String>): Result<ProjectViewSection>

  sealed interface ScalarSectionParser<out T> : ProjectViewSectionParser {
    override fun parse(items: List<String>): Result<Scalar<T>> =
      when {
        items.isEmpty() || items.first() == "" -> Result.failure(Exception("Empty section"))
        items.size > 1 -> Result.failure(Exception("A scalar section should have exactly a single value"))
        else -> parseValue(items.first())
      }

    fun parseValue(value: String): Result<Scalar<T>>

    data object Int : ScalarSectionParser<kotlin.Int> {
      override fun parseValue(value: String): Result<Scalar<kotlin.Int>> =
        value.toIntOrNull()?.let {
          Result.success(Scalar(it, DefaultLanguageHighlighterColors.NUMBER))
        } ?: Result.failure(ScalarValueException("Invalid number"))
    }

    data object Boolean : ScalarSectionParser<kotlin.Boolean> {
      override fun parseValue(value: String): Result<Scalar<kotlin.Boolean>> {
        val boolean =
          when (value) {
            "true" -> true
            "false" -> false
            else -> null
          }

        return boolean?.let { Result.success(Scalar(it, DefaultLanguageHighlighterColors.KEYWORD)) }
          ?: Result.failure(ScalarValueException("Invalid boolean"))
      }
    }

    data object Identifier : Simple

    data object Path : Simple

    private sealed interface Simple : ScalarSectionParser<String> {
      override fun parseValue(value: String): Result<Scalar<String>> = Result.success(Scalar(value, null))
    }
  }

  sealed interface ListSectionParser<out T> : ProjectViewSectionParser {
    override fun parse(items: List<String>): Result<ProjectViewSection.List<T>>

    sealed interface Simple : ListSectionParser<String> {
      override fun parse(items: List<String>): Result<ProjectViewSection.List<String>> =
        if (items.isNotEmpty()) {
          Result.success(ProjectViewSection.List(items, null))
        } else {
          Result.failure(Exception("Empty section"))
        }
    }

    data object Identifiers : Simple

    data object Paths : Simple
  }

  data class ScalarValueException(override val message: String) : Exception()
}
