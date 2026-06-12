package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.JTree
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestApplication
class BazelTargetsPanelTest {
  private val projectFixture = projectFixture()
  private val project by projectFixture

  @Test
  fun `target tree does not install platform speed search`() {
    val panel = createPanel()
    val targetTree = panel.targetTree()

    assertNull(SpeedSearchSupply.getSupply(targetTree, true))
  }

  @Test
  fun `typing in target tree updates toolwindow search query`() {
    val model = createModel()
    val panel = createPanel(model)

    val event1 = panel.typeInTargetTree('q')
    assertTrue(event1.isConsumed)
    assertEquals("q", model.searchQuery())

    val event2 = panel.typeInTargetTree('#')
    assertTrue(event2.isConsumed)
    assertEquals("q#", model.searchQuery())
  }

  @Test
  fun `non printable typed key in target tree is ignored`() {
    val model = createModel()
    val panel = createPanel(model)
    val targetTree = panel.targetTree()
    val event = keyTyped(targetTree, '\n')

    targetTree.keyListeners.forEach { it.keyTyped(event) }

    assertFalse(event.isConsumed)
    assertEquals("", model.searchQuery())
  }

  @Test
  fun `clicking no targets message focuses searchbar`() {
    val model = createModel()
    val panel = createPanel(model)
    panel.showNoTargetsMessage()
    val message = panel.messageLabel()
    val searchBarPanel = mockSearchBarPanel()
    panel.replaceSearchBarPanel(searchBarPanel)
    val event = mouseClicked(message)

    message.dispatchEvent(event)

    searchBarPanel.verifyFocusRequested()
    assertTrue(event.isConsumed)
  }

  private fun createPanel(model: Any = createModel()): JComponent {
    val panelClass = Class.forName("$COMPONENTS_PACKAGE.BazelTargetsPanel")
    val constructor = panelClass.getDeclaredConstructor(Project::class.java, model.javaClass)
    constructor.isAccessible = true
    return constructor.newInstance(project, model) as JComponent
  }

  private fun createModel(): Any {
    val modelClass = Class.forName("$COMPONENTS_PACKAGE.BazelTargetsPanelModel")
    val constructor = modelClass.getDeclaredConstructor(MutableSharedFlow::class.java)
    constructor.isAccessible = true
    return constructor.newInstance(MutableSharedFlow<Unit>(replay = 1))
  }

  private fun JComponent.targetTree(): JComponent =
    assertNotNull(UIUtil.findComponentOfType(this, JTree::class.java))

  private fun JComponent.messageLabel(): JLabel =
    assertNotNull(UIUtil.findComponentOfType(this, JLabel::class.java))

  private fun JComponent.searchBarPanel(): JComponent {
    val field = javaClass.getDeclaredField("searchBarPanel")
    field.isAccessible = true
    return field.get(this) as JComponent
  }

  private fun JComponent.searchTextField(): JTextField =
    assertNotNull(UIUtil.findComponentOfType(searchBarPanel(), JTextField::class.java))

  private fun JComponent.typeInTargetTree(keyChar: Char): KeyEvent {
    val targetTree = targetTree()
    val event = keyTyped(targetTree, keyChar)
    targetTree.keyListeners.forEach { it.keyTyped(event) }
    val searchTextField = searchTextField()
    searchTextField.caretPosition = searchTextField.text.length // this is default behavior in UI, but in tests it needs to be explicit
    return event
  }

  private fun mockSearchBarPanel(): Any =
    mock(Class.forName("$COMPONENTS_PACKAGE.SearchBarPanel"))

  private fun JComponent.replaceSearchBarPanel(searchBarPanel: Any) {
    val field = javaClass.getDeclaredField("searchBarPanel")
    field.isAccessible = true
    field.set(this, searchBarPanel)
  }

  private fun Any.verifyFocusRequested() {
    val method = javaClass.getMethod("requestFocus")
    method.invoke(verify(this))
  }

  private fun JComponent.showNoTargetsMessage() {
    val method =
      javaClass.getDeclaredMethod(
        "update",
        List::class.java,
        Regex::class.java,
        Boolean::class.javaPrimitiveType!!,
        Boolean::class.javaPrimitiveType!!,
      )
    method.isAccessible = true
    method.invoke(this, emptyList<Any>(), null, false, true)
  }

  private fun Any.searchQuery(): String {
    val getter = javaClass.getDeclaredMethod("getSearchQuery")
    getter.isAccessible = true
    return getter.invoke(this) as String
  }

  private fun keyTyped(source: JComponent, keyChar: Char): KeyEvent =
    KeyEvent(source, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, keyChar)

  private fun mouseClicked(source: JComponent): MouseEvent =
    MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1)

  private companion object {
    const val COMPONENTS_PACKAGE = "org.jetbrains.bazel.ui.widgets.tool.window.components"
  }
}
