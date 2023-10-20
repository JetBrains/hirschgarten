package org.jetbrains.bazel.languages.starlark

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class StarlarkColorSettingsPage : ColorSettingsPage {
  override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
    infix fun String.to(key: TextAttributesKey) = AttributesDescriptor(this, key)

    return arrayOf(
      StarlarkBundle.message("highlighter.descriptor.text.keyword") to StarlarkHighlightingColors.KEYWORD,
      StarlarkBundle.message("highlighter.descriptor.text.string") to StarlarkHighlightingColors.STRING,
      StarlarkBundle.message("highlighter.descriptor.text.number") to StarlarkHighlightingColors.NUMBER,
      StarlarkBundle.message("highlighter.descriptor.text.lineComment") to StarlarkHighlightingColors.LINE_COMMENT,
      StarlarkBundle.message("highlighter.descriptor.text.semicolon") to StarlarkHighlightingColors.SEMICOLON,
      StarlarkBundle.message("highlighter.descriptor.text.comma") to StarlarkHighlightingColors.COMMA,
      StarlarkBundle.message("highlighter.descriptor.text.dot") to StarlarkHighlightingColors.DOT,
      StarlarkBundle.message("highlighter.descriptor.text.parentheses") to StarlarkHighlightingColors.PARENTHESES,
      StarlarkBundle.message("highlighter.descriptor.text.brackets") to StarlarkHighlightingColors.BRACKETS,
      StarlarkBundle.message("highlighter.descriptor.text.identifier") to StarlarkHighlightingColors.IDENTIFIER,
      StarlarkBundle.message("highlighter.descriptor.text.functionDeclaration")
        to StarlarkHighlightingColors.FUNCTION_DECLARATION,
      StarlarkBundle.message("highlighter.descriptor.text.namedArgument") to StarlarkHighlightingColors.NAMED_ARGUMENT,
    )
  }

  override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

  override fun getDisplayName(): String = StarlarkLanguage.id

  override fun getIcon(): Icon = StarlarkLanguageIcons.bazel

  override fun getHighlighter(): SyntaxHighlighter = StarlarkSyntaxHighlighter

  override fun getDemoText(): String = """
    |# Line comment
    |
    |def fun(param, optional_param = "string", *varargs, **kwargs):
    |    return 42
    |
    |if x > 0:
    |    result = +1
    |elif x < 0:
    |    result = -1
    |else:
    |    result = 0
    |
    |for x in 1, 2, 3:
    |    print(x)
    |    
    |def func(a, b, c):
    |    return a + b * c
    |    
    |func(c = 3, a = 2, b = 1); func(1, 2, 3)
    |
    |"banana".count("a")
    |numbers = {"one": 1, "two": 2}
  """.trimMargin()

  override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null
}
