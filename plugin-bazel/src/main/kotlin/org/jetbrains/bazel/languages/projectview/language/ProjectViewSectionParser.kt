package org.jetbrains.bazel.languages.projectview.language

sealed interface ProjectViewSectionParser<out SectionT : ProjectViewSection> {
  /**
   * An unparsed section is modelled as a sequence of items.
   */
  data class Item(val value: String) {
    val isEmpty: Boolean = value.isEmpty()
  }

  fun parse(items: Iterable<Item>): Result<SectionT> {
    val iterator = items.iterator()

    return if (iterator.hasNext()) {
      parseNonEmptySection(iterator.next(), iterator)
    } else {
      Result.Failure.EMPTY_SECTION
    }
  }

  /**
   * Parse a section which has at least one item.
   * There are no guaranties whether the items are notEmpty.
   */
  fun parseNonEmptySection(head: Item, tail: Iterator<Item>): Result<SectionT>

  sealed interface Scalar<T> : ProjectViewSectionParser<ProjectViewSection.Scalar<T>> {
    override fun parseNonEmptySection(head: Item, tail: Iterator<Item>): Result<ProjectViewSection.Scalar<T>> =
      if (tail.hasNext()) {
        // This section contains more than one item.
        Result.Failure("A scalar section should contain a single item", Scope.Section)
      } else if (head.isEmpty) {
        Result.Failure.EMPTY_SECTION
      } else {
        parseValue(head).fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Failure(it.message ?: "", Scope.Item(0)) })
      }

    fun parseValue(item: Item): kotlin.Result<ProjectViewSection.Scalar<T>>

    data object Boolean : Scalar<kotlin.Boolean> {
      override fun parseValue(item: Item): kotlin.Result<ProjectViewSection.Scalar.Boolean> {
        val boolean =
          when (item.value) {
            "true" -> true
            "false" -> false
            else -> null
          }

        return boolean?.let { kotlin.Result.success(ProjectViewSection.Scalar.Boolean(it)) }
          ?: kotlin.Result.failure(Exception("Invalid boolean"))
      }
    }

    data object Identifier : Scalar<String> {
      override fun parseValue(item: Item): kotlin.Result<ProjectViewSection.Scalar.Identifier> =
        kotlin.Result.success(ProjectViewSection.Scalar.Identifier(item.value))
    }

    data object Int : Scalar<kotlin.Int> {
      override fun parseValue(item: Item): kotlin.Result<ProjectViewSection.Scalar.Int> =
        item.value.toIntOrNull()?.let {
          kotlin.Result.success(ProjectViewSection.Scalar.Int(it))
        } ?: kotlin.Result.failure(Exception("Invalid number"))
    }

    data object Path : Scalar<String> {
      override fun parseValue(item: Item): kotlin.Result<ProjectViewSection.Scalar.Path> =
        kotlin.Result.success(ProjectViewSection.Scalar.Path(item.value))
    }
  }

  sealed interface List<T> : ProjectViewSectionParser<ProjectViewSection.List<T>> {
    override fun parseNonEmptySection(head: Item, tail: Iterator<Item>): Result<ProjectViewSection.List<T>>

    data object Identifiers : List<String> {
      override fun parseNonEmptySection(head: Item, tail: Iterator<Item>): Result<ProjectViewSection.List<String>> =
        Result.Success(
          ProjectViewSection.List.Identifiers(
            listOf(head.value) + tail.asSequence().map { it.value }.toList(),
          ),
        )
    }

    data object Paths : List<String> {
      override fun parseNonEmptySection(head: Item, tail: Iterator<Item>): Result<ProjectViewSection.List<String>> =
        Result.Success(
          ProjectViewSection.List.Paths(
            listOf(head.value) + tail.asSequence().map { it.value }.toList(),
          ),
        )
    }
  }

  sealed interface Result<out T : ProjectViewSection> {
    data class Success<out T : ProjectViewSection>(val value: T) : Result<T>

    data class Failure(val message: String, val scope: Scope) : Result<Nothing> {
      companion object {
        val EMPTY_SECTION = Failure("Empty section", Scope.Section)
      }
    }
  }

  sealed interface Scope {
    data object Section : Scope

    data class Item(val id: Int) : Scope
  }
}
