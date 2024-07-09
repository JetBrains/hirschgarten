package org.jetbrains.bsp.probe.test

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.stepsProcessing.step
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.WaitDecision
import org.virtuslab.ideprobe.WaitLogic
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import org.virtuslab.ideprobe.robot.SearchableComponent
import org.virtuslab.ideprobe.wait.BasicWaiting
import org.virtuslab.ideprobe.wait.DoOnlyOnce
import scala.Option
import scala.Tuple2
import scala.concurrent.duration.FiniteDuration
import scala.runtime.BoxedUnit
import java.io.InvalidClassException
import java.util.concurrent.TimeUnit
import org.virtuslab.ideprobe.robot.`RobotSyntax$`.`MODULE$` as RobotSyntaxObj
import org.virtuslab.ideprobe.wait.`WaitLogicFactory$`.`MODULE$` as WaitLogicFactory

val robotTimeout: FiniteDuration = FiniteDuration(10, TimeUnit.SECONDS)
val fiveSeconds: FiniteDuration = FiniteDuration(5, TimeUnit.SECONDS)
val minute: FiniteDuration = FiniteDuration(1, TimeUnit.MINUTES)

fun emptyBackgroundTaskWithoutTimeouts(
  basicCheck: FiniteDuration = WaitLogicFactory.DefaultCheckFrequency(),
  ensurePeriod: FiniteDuration = WaitLogicFactory.DefaultEnsurePeriod(),
  ensureFrequency: FiniteDuration = WaitLogicFactory.DefaultEnsureFrequency(),
  atMost: FiniteDuration = WaitLogicFactory.DefaultAtMost(),
  action: () -> Unit,
): WaitLogic =
  WaitLogic.emptyBackgroundTasks(basicCheck, ensurePeriod, ensureFrequency, atMost).doWhileWaiting { _ ->
    action()
    BoxedUnit.UNIT
  }

fun SearchableComponent.fixture(): CommonContainerFixture = searchContext() as? CommonContainerFixture
  ?: throw InvalidClassException("Element ${searchContext()} is not a CommonContainerFixture ")

fun SearchableComponent.fullText(): String = fullTexts().joinToString("\n")

fun SearchableComponent.fullTexts(): List<String> = fixture().findAllText().map { it.text }

fun SearchableComponent.doClick(): Unit = fixture().runJs("component.doClick();", true)

fun SearchableComponent.doubleClick(): Unit = fixture().runJs("robot.doubleClick(component);", true)

fun SearchableComponent.clickElementNamed(
  name: String,
  type: MouseButton,
  count: Int = 1,
  sameNamedInstance: Int = 0
) = with(fixture()) {
  val button = when (type) {
    MouseButton.Left -> "MouseButton.LEFT_BUTTON"
    MouseButton.Right -> "MouseButton.RIGHT_BUTTON"
    else -> return@with
  }
  extractData().filter { it.text == name }.getOrNull(sameNamedInstance)?.run {
    step("clicking $count time(s) with $type at ${point.x}:${point.y}") {
      runJs(
        """
        const point = new java.awt.Point(${point.x}, ${point.y}); 
        robot.click(component, point, $button, $count);
      """.trimIndent()
      )
    }
  }
}

// Probably not ideal selector, but without changes to dialog won't be better
fun RobotProbeDriver.findContextMenu() =
  findElement(Query.div("class" to "MyList"))

fun SearchableComponent.click(): Unit = fixture().runJs("component.click();", true)

fun SearchableComponent.setText(text: String): Unit = fixture().runJs("component.setText('$text');", true)

fun RobotProbeDriver.getBuildConsoleOutput(): List<String> = findElement(Query.className("BuildTextConsoleView"))
  .fixture()
  .callJs<String>("component.getText();")
  .split("\n")

fun SearchableComponent.findElement(xpath: String) =
  RobotSyntaxObj.SearchableOps(this.findWithTimeout(xpath, robotTimeout))

fun IntelliJFixture.withBuild(build: String, version: String? = null) = withVersion(
  `IdeProbeTestRunner$`.`MODULE$`.version(build, Option.apply(version))
)

fun ProbeDriver.tryUntilSuccessful(action: () -> Unit) {
  val actionToDo = DoOnlyOnce {
    action()
    BoxedUnit.UNIT
  }
  val waitLogic = BasicWaiting(fiveSeconds, minute) {
    actionToDo.attempt()
    if (actionToDo.isSuccessful) WaitDecision.`Done$`.`MODULE$`
    else WaitDecision.KeepWaiting(Option.apply(null))
  }
  return await(waitLogic)
}

fun <A, B> Tuple2<A, B>.toKotlin() = Pair(this._1, this._2)

object Query {
  fun dialog(title: String): String = dialog("title" to title)

  fun dialog(vararg attributes: Pair<String, String>): String = div("class" to "MyDialog", *attributes)

  fun button(vararg attributes: Pair<String, String>): String = div("class" to "JButton", *attributes)

  fun button(accessibleName: String, vararg attributes: Pair<String, String>): String =
    button("accessiblename" to accessibleName, *attributes)

  fun radioButton(vararg attributes: Pair<String, String>): String = div("class" to "JRadioButton", *attributes)

  fun radioButton(accessibleName: String, vararg attributes: Pair<String, String>): String =
    radioButton("accessiblename" to accessibleName, *attributes)

  fun className(name: String, vararg attributes: Pair<String, String>): String = div("class" to name, *attributes)

  fun div(vararg attributes: Pair<String, String>): String =
    attributes.joinToString(" and ", "//div[", "]") { (name, value) -> "@$name='$value'" }
}
