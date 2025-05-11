package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection.Scalar

sealed interface ProjectViewSectionParser {
  /**
   * An unparsed section is modelled as a sequence of items.
   */
  data class Item(val value: String)

  fun parse(items: Iterable<Item>): Result {
    val iterator = items.iterator()

    return if (iterator.hasNext()) {
      parseNonEmpty(iterator.next(), iterator)
    } else {
      Result.Failure("Empty section", Scope.Section)
    }
  }

  fun parseNonEmpty(head: Item, tail: Iterator<Item>): Result

  sealed interface ScalarSectionParser<out T> : ProjectViewSectionParser {
    override fun parseNonEmpty(head: Item, tail: Iterator<Item>): Result =
      if (tail.hasNext()) {
        Result.Failure("A scalar section should contain a single item", Scope.Section)
      } else {
        parseValue(head).fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Failure(it.message ?: "", Scope.Item(0)) })
      }

    fun parseValue(item: Item): kotlin.Result<Scalar<T>>

    data object Int : ScalarSectionParser<kotlin.Int> {
      override fun parseValue(item: Item): kotlin.Result<Scalar<kotlin.Int>> =
        item.value.toIntOrNull()?.let {
          kotlin.Result.success(Scalar(it, DefaultLanguageHighlighterColors.NUMBER))
        } ?: kotlin.Result.failure(Exception("Invalid number"))
    }

    data object Boolean : ScalarSectionParser<kotlin.Boolean> {
      override fun parseValue(item: Item): kotlin.Result<Scalar<kotlin.Boolean>> {
        val boolean =
          when (item.value) {
            "true" -> true
            "false" -> false
            else -> null
          }

        return boolean?.let { kotlin.Result.success(Scalar(it, DefaultLanguageHighlighterColors.KEYWORD)) }
          ?: kotlin.Result.failure(Exception("Invalid boolean"))
      }
    }

    data object Identifier : Simple

    data object Path : Simple

    private sealed interface Simple : ScalarSectionParser<String> {
      override fun parseValue(item: Item): kotlin.Result<Scalar<String>> =
        kotlin.Result.success(
          Scalar(
            item.value,
            null,
          ),
        )
    }
  }

  sealed interface ListSectionParser : ProjectViewSectionParser {
    override fun parseNonEmpty(head: Item, tail: Iterator<Item>): Result =
      Result.Success(
        ProjectViewSection.List<String>(
          listOf(head.value) + tail.asSequence().map { it.value }.toList(),
          null,
        ),
      )

    data object Identifiers : ListSectionParser

    data object Paths : ListSectionParser
  }

  sealed interface Result {
    data class Success(val value: ProjectViewSection) : Result

    data class Failure(val message: String, val scope: Scope) : Result
  }

  sealed interface Scope {
    data object Section : Scope

    data class Item(val id: Int) : Scope
  }
}
