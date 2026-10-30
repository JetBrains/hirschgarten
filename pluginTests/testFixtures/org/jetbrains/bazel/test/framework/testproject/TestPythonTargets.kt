package org.jetbrains.bazel.test.framework.testproject

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.StringsCollection
import java.nio.file.Path

fun pythonTarget(
  label: Label,
  kind: String = "py_binary",
  ruleType: RuleType = RuleType.BINARY,
  interpreter: Path? = null,
  imports: List<String> = emptyList(),
  mainFile: Path? = null,
  mainModule: String? = null,
  runnerScript: Path? = null,
  targetArgs: List<String> = emptyList(),
  declare: TestTargetBuilder.() -> Unit = {},
): TestBuildTarget =
  buildTarget(label, TargetKind(kind = kind, ruleType = ruleType, languageClasses = setOf(PythonLanguageClass.PYTHON))) {
    data(
      PythonBuildTarget(
        version = "3",
        interpreter = interpreter?.let { OutputLocation.Host(it.toString()) },
        imports = StringsCollection.of(imports),
        mainFile = mainFile?.let { OutputLocation.Host(it.toString()) },
        mainModule = mainModule,
        runnerScript = runnerScript,
        targetArgs = StringsCollection.of(targetArgs),
      ),
    )
    declare()
  }

fun pythonTarget(
  label: String,
  kind: String = "py_binary",
  ruleType: RuleType = RuleType.BINARY,
  interpreter: Path? = null,
  imports: List<String> = emptyList(),
  mainFile: Path? = null,
  mainModule: String? = null,
  runnerScript: Path? = null,
  targetArgs: List<String> = emptyList(),
  declare: TestTargetBuilder.() -> Unit = {},
): TestBuildTarget =
  pythonTarget(Label.parse(label), kind, ruleType, interpreter, imports, mainFile, mainModule, runnerScript, targetArgs, declare)
