package org.jetbrains.bazel.run.config

import com.intellij.execution.RunManager
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.impl.SingleConfigurationConfigurable
import com.intellij.execution.ui.RunnerAndConfigurationAwareSettingsEditor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

@BazelTestApplication
class BazelRunConfigurationEditorTest {
  private val myFixture by bazelCodeInsightFixture()

  @ParameterizedTest
  @ValueSource(strings = ["//app:unit /", "//app:unit @repo", "//...:unit"])
  fun `target listeners tolerate incomplete text while Apply rejects it`(text: String, @TestDisposable disposable: Disposable): Unit =
    timeoutRunBlocking {
      withContext(Dispatchers.EDT) {
        val configuration = BazelRunConfigurationType().createTemplateConfiguration(myFixture.project) as BazelRunConfiguration
        configuration.updateTargets("//app:unit", RunHandlerProvider.findRunHandlerProvider("GenericRunHandlerProvider"))
        val fragment = TargetsFragment(myFixture.project)
        Disposer.register(disposable, fragment)
        val field = fragment.component().component
        field.setDisposedWith(disposable)
        fragment.resetFrom(configuration)
        assertEquals(configuration.targets, fragment.targets)
        val observedTargets = mutableListOf<List<Label>>()
        fragment.addTargetsListener { observedTargets.add(fragment.targets) }

        field.text = text

        assertEquals(emptyList(), observedTargets.last())
        val error = assertFailsWith<ConfigurationException> { fragment.applyTo(configuration) }
        assertIs<IllegalArgumentException>(error.cause)
        assertEquals("//app:unit", configuration.targetListString)
        assertEquals(listOf(Label.parse("//app:unit")), configuration.targets)

        field.text = "//app:unit //other:test"

        val expectedTargets = listOf(Label.parse("//app:unit"), Label.parse("//other:test"))
        assertEquals(expectedTargets, observedTargets.last())
        fragment.applyTo(configuration)
        assertEquals(expectedTargets, configuration.targets)
      }
    }

  @ParameterizedTest
  @ValueSource(ints = [0, 3, 9])
  fun `handler changes preserve the target cursor and selection`(caretOffset: Int, @TestDisposable disposable: Disposable): Unit =
    timeoutRunBlocking {
      withContext(Dispatchers.EDT) {
        val editorDisposable = Disposer.newDisposable()
        Disposer.register(disposable, editorDisposable)
        try {
          val settings = RunManager.getInstance(myFixture.project)
            .createConfiguration("custom", bazelRunConfigurationFactory) as RunnerAndConfigurationSettingsImpl
          val configuration = settings.configuration as BazelRunConfiguration
          val editor = configuration.configurationEditor
          Disposer.register(editorDisposable, editor)
          (editor as RunnerAndConfigurationAwareSettingsEditor).resetEditorFrom(settings)
          editor.resetFrom(configuration)
          val field = assertNotNull(UIUtil.findComponentOfType(editor.component, TextFieldWithCompletion::class.java))
          field.setDisposedWith(editorDisposable)
          val textEditor = assertNotNull(field.getEditor(true))
          field.text = "//app:bin"
          textEditor.caretModel.moveToOffset(caretOffset)
          if (caretOffset == 3) {
            textEditor.caretModel.primaryCaret.setSelection(3, 6, false)
          }
          assertNull(configuration.handlerProvider)

          editor.applyTo(configuration)

          assertNotNull(configuration.handlerProvider)
          val newField = assertNotNull(UIUtil.findComponentOfType(editor.component, TextFieldWithCompletion::class.java))
          assertNotSame(field, newField)
          newField.setDisposedWith(editorDisposable)
          val newTextEditor = assertNotNull(newField.getEditor(true))
          assertEquals("//app:bin", newField.text)
          assertEquals(caretOffset, newTextEditor.caretModel.offset)
          assertEquals(if (caretOffset == 3) "pp:" else null, newTextEditor.selectionModel.selectedText)
        } finally {
          Disposer.dispose(editorDisposable)
        }
      }
    }

  @ParameterizedTest
  @ValueSource(strings = ["//app:bin /", "//app:bin @repo", "//...:bin"])
  fun `modification checks recover after invalid target text`(text: String): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val manager = RunManager.getInstance(myFixture.project)
      val settings = manager.createConfiguration("custom", bazelRunConfigurationFactory)
      val configuration = settings.configuration as BazelRunConfiguration
      val provider = assertNotNull(RunHandlerProvider.findRunHandlerProvider("GenericRunHandlerProvider"))
      configuration.updateTargets("//app:original", provider)
      manager.addConfiguration(settings)
      val configurable = SingleConfigurationConfigurable.editSettings<BazelRunConfiguration>(settings, null)
      try {
        val field = assertNotNull(UIUtil.findComponentOfType(configurable.createComponent(), TextFieldWithCompletion::class.java))
        assertFalse(configurable.isModified)

        field.text = text
        repeat(2) {
          assertTrue(configurable.isModified)
          val error = assertFailsWith<ConfigurationException> { configurable.apply() }
          assertIs<IllegalArgumentException>(error.cause)
          assertEquals("//app:original", configuration.targetListString)
          assertEquals(listOf(Label.parse("//app:original")), configuration.targets)
        }

        val correctedText = "  //app:other  "
        field.text = correctedText
        assertTrue(configurable.isModified)
        configurable.apply()

        assertEquals(correctedText, configuration.targetListString)
        assertEquals(listOf(Label.parse("//app:other")), configuration.targets)
        assertFalse(configurable.isModified)
      } finally {
        configurable.disposeUIResources()
        manager.removeConfiguration(settings)
      }
    }
  }
}
