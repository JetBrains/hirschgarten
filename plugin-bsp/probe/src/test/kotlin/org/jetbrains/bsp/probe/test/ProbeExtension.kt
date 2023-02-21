package org.jetbrains.bsp.probe.test

import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.OS
import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import org.virtuslab.ideprobe.robot.SearchableComponent
import scala.Option
import scala.concurrent.duration.FiniteDuration
import java.io.InvalidClassException
import java.util.concurrent.TimeUnit
import org.virtuslab.ideprobe.robot.`RobotSyntax$`.`MODULE$` as RobotSyntaxObj

val robotTimeout: FiniteDuration = FiniteDuration(10, TimeUnit.SECONDS)

fun SearchableComponent.fixture(): CommonContainerFixture = searchContext() as? CommonContainerFixture
  ?: throw InvalidClassException("Element ${searchContext()} is not a CommonContainerFixture ")

fun SearchableComponent.fullText(): String = fullTexts().joinToString("\n")

fun SearchableComponent.fullTexts(): List<String> = fixture().findAllText().map(RemoteText::text)

fun SearchableComponent.doClick(): Unit = fixture().runJs("component.doClick();", true)

fun SearchableComponent.click(): Unit = fixture().runJs("component.click();", true)

fun SearchableComponent.setText(text: String): Unit = fixture().runJs("component.setText('$text');", true)

fun RobotProbeDriver.getBuildConsoleOutput(): List<String> = findElement(query.className("BuildTextConsoleView"))
    .fixture()
    .callJs<String>("component.getText();")
    .split("\n")

fun SearchableComponent.findElement(xpath: String) =
  RobotSyntaxObj.SearchableOps(this.findWithTimeout(xpath, robotTimeout))

fun IntelliJFixture.withBuild(build: String) = withVersion(
  IntelliJVersion(
    build, Option.apply(null), if (OS.Current() == OS.`Mac$`.`MODULE$`) ".dmg" else ".zip"
  )
)

object query {

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
