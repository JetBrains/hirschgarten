package org.jetbrains.bazel.test.framework.annotation

import org.jetbrains.bazel.test.framework.BazelVersionedTest
import org.jetbrains.bazel.test.framework.ext.EnabledOnBazelCondition
import org.junit.jupiter.api.extension.ExtendWith

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@ExtendWith(EnabledOnBazelCondition::class)
annotation class EnabledOnBazel(vararg val versions: String)
