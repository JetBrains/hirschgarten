package org.jetbrains.bazel.fixtures

import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.test.framework.BazelTestApplication

// order of annotations here matters!
@SystemPropertyClassLevel(BazelCLionFeatureFlags.CLION_ENABLED, "true")
@SystemPropertyClassLevel(BazelFeatureFlags.USE_PTY, "false") // otherwise tests fail due to a leaked timer
@BazelTestApplication
annotation class CcTestApplication
