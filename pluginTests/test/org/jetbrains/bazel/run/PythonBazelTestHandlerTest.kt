package org.jetbrains.bazel.run

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.python.run.PythonBazelTestHandler
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TestParams
import org.junit.jupiter.api.Test

internal class PythonBazelTestHandlerTest {
  @Test
  fun `should add --junitxml options to environment variables`() {
    val initialEnvironment = mapOf("Key1" to "Value1", "Key2" to "Value2")
    val newEnvironment = transformEnvironment(initialEnvironment).shouldNotBeNull()
    newEnvironment["PYTEST_ADDOPTS"].shouldBe($$"--junitxml=${XML_OUTPUT_FILE} -o junit_family=xunit1")
  }

  @Test
  fun `should add --junitxml options to empty environment variables`() {
    val newEnvironment = transformEnvironment(emptyMap()).shouldNotBeNull()
    newEnvironment["PYTEST_ADDOPTS"].shouldBe($$"--junitxml=${XML_OUTPUT_FILE} -o junit_family=xunit1")
  }

  @Test
  fun `should add --junitxml options to existing PYTEST_ADDOPTS environment variables`() {
    val initialEnvironment = mapOf("Key1" to "Value1", "PYTEST_ADDOPTS" to "--flag -o key=value")
    val newEnvironment = transformEnvironment(initialEnvironment).shouldNotBeNull()
    newEnvironment["PYTEST_ADDOPTS"].shouldBe($$"--flag -o key=value --junitxml=${XML_OUTPUT_FILE} -o junit_family=xunit1")
  }

  @Test
  fun `should not overwrite existing --junitxml options`() {
    val initialEnvironment =
      mapOf("Key1" to "Value1", "PYTEST_ADDOPTS" to "--flag --junitxml=/home/user/log.xml -o key=value")
    val newEnvironment = transformEnvironment(initialEnvironment).shouldNotBeNull()
    newEnvironment shouldContainExactly initialEnvironment
  }
}

private fun transformEnvironment(initialEnvironment: Environment?): Environment? {
  val params =
    TestParams(
      taskId = TaskGroupId.EMPTY.task(""),
      targets = emptyList(),
      environmentVariables = initialEnvironment
    )
  val transformed = PythonBazelTestHandler.addJunitXmlOptionsToEnvironment(params)
  return transformed.environmentVariables
}

private typealias Environment = Map<String, String>
