package org.jetbrains.bazel.run.commandLine

import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
fun parseAsProgramArguments(value: String?): List<String> =
  if (value == null) emptyList()
  else ParametersListUtil.parse(
    /* parameterString = */ value,
    /* keepQuotes = */ false,
    /* supportSingleQuotes = */ true,
  )
