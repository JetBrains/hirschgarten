package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.testFramework.junit5.fixture.TestFixtures
import com.intellij.testFramework.junit5.impl.TestApplicationExtension
import org.jetbrains.annotations.TestOnly
import org.junit.jupiter.api.extension.ExtendWith

@TestOnly
@Target(AnnotationTarget.CLASS)
@ExtendWith(
  TestApplicationExtension::class, BazelIdeaTextExtension::class,
)
@TestFixtures
annotation class BazelTestApplication
