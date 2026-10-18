package org.jetbrains.bazel.test.framework.testproject

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.StringsCollection

fun pythonTarget(
  label: Label,
  kind: String = "py_binary",
  ruleType: RuleType = RuleType.BINARY,
  declare: TestTargetBuilder.() -> Unit = {},
): TestBuildTarget =
  buildTarget(label, TargetKind(kind = kind, ruleType = ruleType, languageClasses = setOf(PythonLanguageClass.PYTHON))) {
    data(PythonBuildTarget(version = "3", interpreter = null, imports = StringsCollection.EMPTY))
    declare()
  }

fun pythonTarget(
  label: String,
  kind: String = "py_binary",
  ruleType: RuleType = RuleType.BINARY,
  declare: TestTargetBuilder.() -> Unit = {},
): TestBuildTarget =
  pythonTarget(Label.parse(label), kind, ruleType, declare)
