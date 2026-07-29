package com.example.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class TableProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val annotated = resolver.getSymbolsWithAnnotation("com.example.processor.GenerateTable")
      .filterIsInstance<KSClassDeclaration>()
      .toList()

    for (declaration in annotated) {
      val containingFile = declaration.containingFile ?: continue
      val packageName = declaration.packageName.asString()
      val objectName = declaration.simpleName.asString() + "Table"

      codeGenerator
        .createNewFile(Dependencies(aggregating = false, containingFile), packageName, objectName)
        .bufferedWriter()
        .use { writer ->
          writer.appendLine("package $packageName")
          writer.appendLine()
          writer.appendLine("internal object $objectName {")
          writer.appendLine("  const val NAME: String = \"${declaration.simpleName.asString().lowercase()}\"")
          writer.appendLine()
          writer.appendLine("  // Generated code reading an `internal` hand-written declaration.")
          writer.appendLine("  fun qualifiedName(): String = TABLE_PREFIX + NAME")
          writer.appendLine("}")
        }
    }

    return emptyList()
  }
}
