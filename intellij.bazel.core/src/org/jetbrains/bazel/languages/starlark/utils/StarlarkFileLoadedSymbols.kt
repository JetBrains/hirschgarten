package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.getSimpleNameOrNull
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

@ApiStatus.Internal
class StarlarkLoadedSymbol(
  val element: PsiElement,
  val localName: String,
  private val loadValue: StarlarkLoadValue,
) {
  val staticValueKind: StarlarkStaticValueKind? by lazy(LazyThreadSafetyMode.NONE) {
    val declaration = loadValue.getLoadValueExpression()?.reference?.resolve() as? StarlarkTargetExpression ?: return@lazy null
    val assignedValue = StarlarkAssignmentUtils.assignedValueForTarget(declaration) ?: return@lazy null
    StarlarkStaticValueAnalyzer.valueKind(assignedValue)
  }
}

@ApiStatus.Internal
class StarlarkFileLoadedSymbols private constructor(symbols: List<StarlarkLoadedSymbol>) {
  val isEmpty: Boolean = symbols.isEmpty()

  private val byName: Map<String, StarlarkLoadedSymbol> = symbols.associateBy { it.localName }

  private val byElement: Map<PsiElement, StarlarkLoadedSymbol> = buildMap {
    for (symbol in symbols) {
      put(symbol.element, symbol)
      symbol.element.parent?.let { put(it, symbol) }
    }
  }

  fun findByReference(element: PsiElement): StarlarkLoadedSymbol? {
    val name = element.getSimpleNameOrNull() ?: return null
    val candidate = byName[name] ?: return null
    val resolved = element.reference?.resolve()
    if (resolved != null) {
      byElement[resolved]?.let { return it }
      if (resolved.containingFile == element.containingFile) return null
    }
    return candidate
  }

  companion object {
    fun create(file: StarlarkFile): StarlarkFileLoadedSymbols {
      val symbols = file.children
        .filterIsInstance<StarlarkLoadStatement>()
        .flatMap { it.getLoadedSymbolsPsi().filterIsInstance<StarlarkLoadValue>() }
        .mapNotNull(::loadedSymbol)
      return StarlarkFileLoadedSymbols(symbols)
    }

    private fun loadedSymbol(value: StarlarkLoadValue): StarlarkLoadedSymbol? =
      when (value) {
        is StarlarkNamedLoadValue -> {
          val name = value.name ?: return null
          StarlarkLoadedSymbol(value.nameIdentifier ?: value, name, value)
        }
        is StarlarkStringLoadValue -> {
          val name = value.getLoadValueExpressionContent() ?: return null
          StarlarkLoadedSymbol(value.getLoadValueExpression() ?: value, name, value)
        }
        else -> null
      }
  }
}
