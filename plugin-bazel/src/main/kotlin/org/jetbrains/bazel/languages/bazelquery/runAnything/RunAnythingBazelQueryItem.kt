package org.jetbrains.bazel.languages.bazelquery.runAnything

import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunction
import org.jetbrains.bazel.languages.bazelquery.options.BazelQueryCommonOptions
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.help
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Icon
import javax.swing.JPanel

class RunAnythingBazelQueryItem(command: String, icon: Icon?) : RunAnythingItemBase(command, icon) {

  override fun createComponent(pattern: String?, isSelected: Boolean, hasFocus: Boolean): Component {
    val command = getCommand()
    val component = super.createComponent(pattern, isSelected, hasFocus) as JPanel

    val toComplete = StringUtil.substringAfterLast(command, " ") ?: ""
    if (toComplete.startsWith("-")) {
      val knownOptions = BazelQueryCommonOptions.getAll()
      val option = knownOptions.firstOrNull {
        "--${it.name.split("=").first()}".startsWith(toComplete) || "-${it.name.split("=").first()}".startsWith(toComplete)
      }
      if (option != null) {
        val flag = Flag.byName("--${option.name.split("=").first()}")
        if (flag != null) {
          val description = flag.help()
          if (description.isNotEmpty()) {
            component.add(
              createDescriptionComponent(description),
              BorderLayout.EAST
            )
          }
        }
      }
    } else if (toComplete.isFunction()) {
      val functionName = toComplete.substringBefore('(')
      val function = BazelQueryFunction.getAll()
        .firstOrNull { it.name == functionName }
      if (function != null) {
        val arguments = function.arguments.joinToString(separator = ", ") { it.name }
        if (arguments.isNotEmpty()) {
          component.add(
            createArgsComponent("$arguments )"),
            BorderLayout.CENTER
          )
        }
        val description = function.description
        if (description.isNotEmpty()) {
          component.add(
            createDescriptionComponent(description),
            BorderLayout.EAST,
          )
        }
      }
    }
    return component
  }

  private fun createArgsComponent(args: String): SimpleColoredComponent {
    val argsComponent = SimpleColoredComponent()
    argsComponent.append(
      StringUtil.shortenTextWithEllipsis(args, 100, 0),
      SimpleTextAttributes.GRAYED_ATTRIBUTES,
    )
    return argsComponent
  }

  private fun createDescriptionComponent(description: String): SimpleColoredComponent {
    val descriptionComponent = SimpleColoredComponent()
    descriptionComponent.append(
      " " + StringUtil.shortenTextWithEllipsis(description, 150, 0),
      SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES,
    )
    return descriptionComponent
  }

  private fun String.isFunction(): Boolean =
    !startsWith("-") && BazelQueryFunction.getAll().any {
      this.startsWith("${it.name}(")
    }
}
