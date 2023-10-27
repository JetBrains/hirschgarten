package org.jetbrains.bazel.debug.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.text.AttributeSet
import javax.swing.text.Document
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument

class StarlarkDebugSettingsEditor : SettingsEditor<StarlarkDebugConfiguration>() {
  private val portField = NumberField(DEFAULT_PORT.toString())
  private val portLabel = createLabelFor(StarlarkBundle.message("starlark.debug.config.port"), portField)
  private val portBox = JPanel().apply {
    add(portLabel)
    add(portField)
  }

  private val commandField = JTextArea()

  init {
    installPortValidation()
    commandField.apply {
      lineWrap = true
      wrapStyleWord = true
      rows = 2
      isEditable = false
      border = SideBorder(JBColor.border(), SideBorder.ALL)
      minimumSize = this.preferredSize
      addFocusListener(SelectAllFocusAdapter(this))
    }
    regenerateCommand()
  }

  private fun createLabelFor(text: String?, component: JComponent): JLabel {
    val label = JLabel()
    LabeledComponent.TextWithMnemonic.fromTextWithMnemonic(text).setToLabel(label)
    label.labelFor = component
    return label
  }

  override fun resetEditorFrom(config: StarlarkDebugConfiguration) {
    portField.text = config.getPort().toString()
  }

  override fun applyEditorTo(config: StarlarkDebugConfiguration) {
    portField.text.toIntOrNull()?.let{ config.setPort(it) }
  }

  override fun createEditor(): JComponent {
    val configPanel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints().apply {
      gridy = 0
      anchor = GridBagConstraints.WEST
    }
    configPanel.add(portBox, constraints)

    constraints.gridy++
    constraints.weightx = 1.0
    constraints.fill = GridBagConstraints.HORIZONTAL
    configPanel.add(commandField, constraints)

    return configPanel
  }

  private fun installPortValidation() {
    ComponentValidator(this)
      .withValidator(ValidationInfoSupplier(portField))
      .installOn(portField)
    portField.let {
      it.document.addDocumentListener(
        DocumentChangeListener(it) { regenerateCommand() }
      )
      it
    }
  }

  private fun regenerateCommand() {
    val portFlag = portField.text.let {
      when {
        it == DEFAULT_PORT.toString() -> ""
        it.portNumberIsCorrect() -> createPortFlag(it)
        else -> createPortFlag(null)
      }
    }
    commandField.text = "bazel build --experimental_skylark_debug$portFlag --nobuild @//..."
  }

  private fun createPortFlag(port: String?): String {
    val portOrPlaceholder = port ?: StarlarkBundle.message("starlark.debug.port.placeholder")
    return " --experimental_skylark_debug_server_port=$portOrPlaceholder"
  }
}


private class NumberField(defaultText: String) : JTextField(defaultText) {
  override fun createDefaultModel(): Document {
    return NumberFieldDocument()
  }

  private class NumberFieldDocument : PlainDocument() {
    override fun insertString(offs: Int, str: String?, a: AttributeSet?) {
      val onlyNumbers =
        str
          ?.filter { it.isDigit() }
          ?.let { crop(it) }
      super.insertString(offs, onlyNumbers, a)
    }

    private fun crop(newText: String): String =
      when {
        this.length >= MAX_PORT_STRING_LEN -> ""
        else -> newText.take(MAX_PORT_STRING_LEN - this.length)
      }
  }
}

private class SelectAllFocusAdapter(private val textField: JTextComponent) : FocusAdapter() {
  override fun focusGained(e: FocusEvent?) {
    textField.selectAll()
  }
}

private class ValidationInfoSupplier(private val portField: JTextField) : Supplier<ValidationInfo?> {
  override fun get(): ValidationInfo? =
    when (portField.text.portNumberIsCorrect()) {
      true -> null
      false -> ValidationInfo(
        StarlarkBundle.message("starlark.debug.port.out.of.range", MIN_PORT_VALUE, MAX_PORT_VALUE),
        portField
      )
    }
}

private class DocumentChangeListener(
  private val textComponent: JTextComponent,
  private val onChange: () -> Unit,
) : DocumentAdapter() {
  override fun textChanged(e: DocumentEvent) {
    ComponentValidator.getInstance(textComponent).ifPresent { it.revalidate() }
    onChange()
  }
}

private fun String.portNumberIsCorrect(): Boolean {
  val port = this.toIntOrNull()
  return port != null && port in MIN_PORT_VALUE..MAX_PORT_VALUE
}

private const val MIN_PORT_VALUE = 0
private const val MAX_PORT_VALUE = 0xFFFF
private const val DEFAULT_PORT = 7300

private const val MAX_PORT_STRING_LEN = MAX_PORT_VALUE.toString().length
