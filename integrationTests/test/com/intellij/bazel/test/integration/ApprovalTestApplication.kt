package com.intellij.bazel.test.integration

import com.intellij.testFramework.junit5.fixture.TestFixtures
import com.intellij.testFramework.junit5.impl.TestApplicationExtension
import org.jetbrains.annotations.TestOnly
import org.junit.jupiter.api.extension.ExtendWith

@TestOnly
@Target(AnnotationTarget.CLASS)
@ExtendWith(
  TestApplicationExtension::class,
)
@TestFixtures
annotation class ApprovalTestApplication
