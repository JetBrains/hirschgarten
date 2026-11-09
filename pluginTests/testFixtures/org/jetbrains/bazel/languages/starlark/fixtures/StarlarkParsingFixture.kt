package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.openapi.project.Project
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.ParsingTestFixture
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.testFramework.junit5.fixture.TestFixture
import org.jetbrains.bazel.languages.starlark.parser.StarlarkParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager

fun starlarkParsingFixture(projectFixture: TestFixture<Project>, baseDir: String): TestFixture<ParsingTestFixture> =
  parsingFixture(
    projectFixture,
    BazelPathManager.getTestFixture("starlark/parser") + "/" + baseDir,
    "bzl",
    definitions = arrayOf(StarlarkParserDefinition()),
  )
