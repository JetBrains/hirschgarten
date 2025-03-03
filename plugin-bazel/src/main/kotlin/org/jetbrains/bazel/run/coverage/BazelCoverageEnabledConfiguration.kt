package org.jetbrains.bazel.run.coverage

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration

class BazelCoverageEnabledConfiguration(configuration: RunConfigurationBase<*>) :
  CoverageEnabledConfiguration(
    configuration,
    BazelCoverageRunner.getInstance(),
  )
