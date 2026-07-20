package org.jetbrains.bazel.python.run

import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.target.TargetSummary

internal data class PythonTarget(val summary: TargetSummary, val data: PythonBuildTarget?)
