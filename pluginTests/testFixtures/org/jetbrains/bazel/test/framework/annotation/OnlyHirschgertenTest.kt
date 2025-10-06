package org.jetbrains.bazel.test.framework.annotation

import org.jetbrains.bazel.test.framework.ext.HirschgartenOnlyTestCondition
import org.junit.jupiter.api.extension.ExtendWith

@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(HirschgartenOnlyTestCondition::class)
annotation class OnlyHirschgertenTest
