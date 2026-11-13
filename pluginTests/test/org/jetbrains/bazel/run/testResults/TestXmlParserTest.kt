package org.jetbrains.bazel.run.testResults

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.Printable
import com.intellij.execution.testframework.Printer
import com.intellij.execution.testframework.sm.runner.GeneralToSMTRunnerEventsConvertor
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.ProjectManager
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

/** The test tree that the service messages of the test console build. */
class TestConsoleTree {
  val root = SMTestProxy.SMRootTestProxy()
  private val converter =
    OutputToGeneralTestEventsConverter("Bazel", false, false, true).apply {
      setProcessor(GeneralToSMTRunnerEventsConvertor(ProjectManager.getInstance().defaultProject, root, "Bazel"))
    }

  /** All nodes except the root, in the order they were started. */
  val nodes: List<SMTestProxy> get() = root.allTests.drop(1)

  fun process(text: String, isError: Boolean) {
    converter.process(if (text.endsWith("\n")) text else "$text\n", if (isError) ProcessOutputType.STDERR else ProcessOutputType.STDOUT)
  }
}

fun SMTestProxy.output(): String =
  buildString {
    printOn(object : Printer {
      override fun print(text: String, contentType: ConsoleViewContentType) {
        append(text)
      }

      override fun onNewAvailable(printable: Printable) {
        printable.printOn(this)
      }

      override fun printHyperlink(text: String, info: HyperlinkInfo?) {
        append(text)
      }

      override fun mark() {}
    })
  }

class MockBuildTaskEventsHandler : BazelTaskEventsHandler {
  private val tree = TestConsoleTree()
  val testNodes: List<SMTestProxy> get() = tree.nodes

  override fun onBuildLogMessage(p0: LogMessageParams) {}

  override fun onBuildPublishDiagnostics(p0: PublishDiagnosticsParams) {}

  override fun onBuildTaskStart(p0: TaskStartParams) {}

  override fun onBuildTaskFinish(p0: TaskFinishParams) {}

  override fun onPublishCoverageReport(report: CoverageReport) {}

  override fun onCachedTestLog(testLog: CachedTestLog) {}

  override fun onTestConsoleOutput(taskId: TaskId, text: String, isError: Boolean) {
    tree.process(text, isError)
  }
}

@BazelTestApplication
class TestXmlParserTest {
  private val javaTargetKind = TargetKindService.getInstance().findPredefinedRule("java_test")
  private val pythonTargetKind = TargetKindService.getInstance().findPredefinedRule("py_test")

  @Test
  fun `pytest, all passing`(
    @TempDir tempDir: Path,
  ) {
    // given
    val samplePassingContents =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <testsuites>
          <testsuite name="test_suite" errors="0" failures="0" skipped="0" tests="4" time="0.080"
                     timestamp="2024-05-17T19:23:31.616701" hostname="test-host">
              <testcase classname="com.example.tests.TestClass" name="test_method_1" time="0.015" />
              <testcase classname="com.example.tests.TestClass" name="test_method_2" time="0.013" />
              <testcase classname="com.example.tests.TestClass" name="test_method_3" time="0.001" />
              <testcase classname="com.example.tests.TestClass" name="test_method_4" time="0.012" />
          </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, samplePassingContents), pythonTargetKind)

    // then
    client.testNodes.size shouldBe 5

    val expectedNames =
      listOf(
        "test_suite",
        "test_method_1",
        "test_method_2",
        "test_method_3",
        "test_method_4",
      )

    client.testNodes.map { it.name } shouldContainExactlyInAnyOrder expectedNames
    client.testNodes.filterNot { it.isSuite }.map { it.isPassed shouldBe true }
  }

  @Test
  fun `pytest, with failures`(
    @TempDir tempDir: Path,
  ) {
    // given
    val samplePassingContents =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <testsuites>
              <testsuite name="mysuite" errors="0" failures="2" skipped="0" tests="22" time="0.163"
                      timestamp="2024-05-21T14:16:34.122101" hostname="test-host">
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_config_1" time="0.001" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_config_2" time="0.001" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_config_3" time="0.000" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_config_4" time="0.000" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_config_5" time="0.001" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_config_6" time="0.001"><skipped></skipped></testcase>
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_config_incorrect_format" time="0.001">
                              <failure
                                      message="AssertionError: assert 'yaml' == 'other'&#10;  - other&#10;  + yaml">def
                                      test_config_incorrect_format():
                                      ""${'"'}Verify data""${'"'}
                                      config = load_test_config('sample')

                                      assert config.get('sample.name') == 'myname'
                                      &gt; assert config.get('sample.format') == 'other'
                                      E AssertionError: assert 'yaml' == 'other'
                                      E - other
                                      E + yaml

                                      sample/core/config/tests/test_config.py:100: AssertionError</failure>
                      </testcase>
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_other_file" time="0.001" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_sample_file" time="0.001" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_other_value" time="0.001">
                              <failure
                                      message="AssertionError: assert None is True&#10; +  where None = &lt;bound method Configuration.get of &lt;sample.core.config.FileConfiguration object at 0x7f6c802f3730&gt;&gt;('pigs_go_oink')&#10; +    where &lt;bound method Configuration.get of &lt;sample.core.config.FileConfiguration object at 0x7f6c802f3730&gt;&gt; = &lt;sample.core.config.FileConfiguration object at 0x7f6c802f3730&gt;.get">def
                                      test_other_value():
                                      ""${'"'}Test other_value() updates values.""${'"'}
                                      config = load_test_config('sample')

                                      assert config.get('sample') is None
                                      config.value({'sample': 'yes'})

                                      E AssertionError: assert None is True
                                      E + where None = &lt;bound method Configuration.get of
                                      &lt;sample.core.config.FileConfiguration object at
                                      0x7f6c802f3730&gt;&gt;('sample')
                                      E + where &lt;bound method Configuration.get of
                                      &lt;sample.core.config.FileConfiguration object at
                                      0x7f6c802f3730&gt;&gt; = &lt;sample.core.config.FileConfiguration
                                      object at 0x7f6c802f3730&gt;.get
                                      sample/core/config/tests/test_config.py:137: AssertionError</failure>
                      </testcase>
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_sample_3" time="0.001" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_sample_4" time="0.001" />
                      <testcase classname="sample.core.config.tests.test_config"
                              name="test_sample_5" time="0.001" />
              </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, samplePassingContents), pythonTargetKind)

    // then
    client.testNodes.size shouldBe 14

    val expectedNames =
      listOf(
        "test_config_1",
        "test_config_2",
        "test_config_3",
        "test_config_4",
        "test_config_5",
        "test_config_6",
        "test_config_incorrect_format",
        "test_other_file",
        "test_sample_file",
        "test_other_value",
        "test_sample_3",
        "test_sample_4",
        "test_sample_5",
        "mysuite",
      )

    client.testNodes.map { it.name } shouldContainExactlyInAnyOrder expectedNames
    client.testNodes.map {
      val data = it
      when (data.name) {
        "mysuite" -> {
          data.isSuite shouldBe true
        }
        "test_other_value", "test_config_incorrect_format" -> {
          data.isDefect shouldBe true
          data.errorMessage shouldContain "AssertionError"
        }
        "test_config_6" -> {
          data.isIgnored shouldBe true
        }
        else -> {
          data.isPassed shouldBe true
        }
      }
    }
  }

  @Test
  fun `junit, all passing`(
    @TempDir tempDir: Path,
  ) {
    // given
    val samplePassingContents =
      """
      <?xml version='1.0' encoding='UTF-8'?>
      <testsuites>
        <testsuite name='com.example.testing.base.Tests' timestamp='2024-05-14T19:23:32.883Z' hostname='localhost' tests='20' failures='0' errors='0' time='13.695' package='' id='0'>
          <properties />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite1' timestamp='2024-05-14T19:23:32.883Z' hostname='localhost' tests='4' failures='0' errors='0' time='0.065' package='' id='1'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite1' time='0.032' />
          <testcase name='test2' classname='com.example.optimization.TestSuite1' time='0.019' />
          <testcase name='test3' classname='com.example.optimization.TestSuite1' time='0.002' />
          <testcase name='test4' classname='com.example.optimization.TestSuite1' time='0.003' />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite2' timestamp='2024-05-14T19:23:32.952Z' hostname='localhost' tests='5' failures='0' errors='0' time='13.528' package='' id='2'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite2' time='3.101' />
          <testcase name='test2' classname='com.example.optimization.TestSuite2' time='4.368' />
          <testcase name='test3' classname='com.example.optimization.TestSuite2' time='1.778' />
          <testcase name='test4' classname='com.example.optimization.TestSuite2' time='1.838' />
          <testcase name='test5' classname='com.example.optimization.TestSuite2' time='2.434' />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite3' timestamp='2024-05-14T19:23:46.484Z' hostname='localhost' tests='1' failures='0' errors='0' time='0.004' package='' id='3'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite3' time='0.004' />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite4' timestamp='2024-05-14T19:23:46.491Z' hostname='localhost' tests='1' failures='0' errors='0' time='0.0' package='' id='4'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite4' time='0.0' />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite5' timestamp='2024-05-14T19:23:46.494Z' hostname='localhost' tests='1' failures='0' errors='0' time='0.0' package='' id='5'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite5' time='0.0' />
          <system-out />
          <system-err />
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, samplePassingContents), javaTargetKind)

    // then
    client.testNodes.size shouldBe 18 // one of the test suites in the XML is empty, so we hide it

    val expectedNames =
      listOf(
        "com.example.optimization",
        "TestSuite1",
        "test1",
        "test2",
        "test3",
        "test4",
        "TestSuite2",
        "test1",
        "test2",
        "test3",
        "test4",
        "test5",
        "TestSuite3",
        "test1",
        "TestSuite4",
        "test1",
        "TestSuite5",
        "test1",
      )

    client.testNodes.map { it.name } shouldContainExactlyInAnyOrder expectedNames
    client.testNodes.filterNot { it.isSuite }.map { it.isPassed shouldBe true }
  }

  @Test
  fun `junit4, with skip and failures`(
    @TempDir tempDir: Path,
  ) {
    // given
    val samplePassingContents =
      """
      <?xml version='1.0' encoding='UTF-8'?>
      <testsuites>
        <testsuite name='com.example.testing.base.Tests' timestamp='2024-05-21T14:59:39.108Z' hostname='localhost' tests='20' failures='1' errors='0' time='13.06' package='' id='0'>
          <properties />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite1' timestamp='2024-05-21T14:59:39.108Z' hostname='localhost' tests='4' failures='1' errors='0' time='0.058' package='' id='1'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite1' time='0.027' />
          <testcase name='sampleFailedTest' classname='com.example.optimization.TestSuite1' time='0.021'>
            <failure message='expected:&lt;0.6946&gt; but was:&lt;0.5946238516952311&gt;' type='java.lang.AssertionError'>java.lang.AssertionError: expected:&lt;0.6946&gt; but was:&lt;0.5946238516952311&gt;
          at org.junit.Assert.fail(Assert.java:88)
          at org.junit.Assert.failNotEquals(Assert.java:834)
          at org.junit.Assert.assertEquals(Assert.java:553)
          at org.junit.Assert.assertEquals(Assert.java:683)
          at com.example.optimization.TestSuite1.test2(TestSuite1.java:141)
          at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
          at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
          at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
          at java.base/java.lang.reflect.Method.invoke(Method.java:566)
          at org.junit.runners.model.FrameworkMethod${'$'}1.runReflectiveCall(FrameworkMethod.java:50)
          at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
          at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
          at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
          at com.example.testing.base.mode.TestModeCheckRule1.evaluate(TestModeCheckRule.java:39)
          at org.junit.rules.RunRules.evaluate(RunRules.java:20)
          at org.junit.internal.runners.statements.FailOnTimeoutCallableStatement.call(FailOnTimeout.java:298)
          at org.junit.internal.runners.statements.FailOnTimeoutCallableStatement.call(FailOnTimeout.java:292)
          at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
          at java.base/java.lang.Thread.run(Thread.java:829)
      </failure></testcase>
          <testcase name='test3' classname='com.example.optimization.TestSuite1' time='0.001' />
          <testcase name='test4' classname='com.example.optimization.TestSuite1' time='0.002' />
          <testcase name='sampleSkippedTest' classname='com.example.optimization.TestSuite1' time='0.0' >
            <skipped />
          </testcase>
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite2' timestamp='2024-05-21T14:59:39.169Z' hostname='localhost' tests='5' failures='0' errors='0' time='12.916' package='' id='2'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite2' time='2.917' />
          <testcase name='test2' classname='com.example.optimization.TestSuite2' time='4.176' />
          <testcase name='test3' classname='com.example.optimization.TestSuite2' time='1.697' />
          <testcase name='test4' classname='com.example.optimization.TestSuite2' time='1.797' />
          <testcase name='test5' classname='com.example.optimization.TestSuite2' time='2.321' />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite3' timestamp='2024-05-21T14:59:52.089Z' hostname='localhost' tests='1' failures='0' errors='0' time='0.003' package='' id='3'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite3' time='0.003' />
          <system-out />
          <system-err />
        </testsuite>
        <testsuite name='com.example.optimization.TestSuite4' timestamp='2024-05-21T14:59:52.095Z' hostname='localhost' tests='1' failures='0' errors='0' time='0.0' package='' id='4'>
          <properties />
          <testcase name='test1' classname='com.example.optimization.TestSuite4' time='0.0' />
          <system-out />
          <system-err />
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    val taskId = TaskGroupId.EMPTY.task("")
    TestXmlParser(client).parseAndReport(taskId, writeTempFile(tempDir, samplePassingContents), javaTargetKind)

    // then
    val expectedTestHints = listOf(
      null,
      "java:suite://com.example.optimization.TestSuite1",
      "java:test://com.example.optimization.TestSuite1/test1",
      "java:test://com.example.optimization.TestSuite1/sampleFailedTest",
      "java:test://com.example.optimization.TestSuite1/test3",
      "java:test://com.example.optimization.TestSuite1/test4",
      "java:test://com.example.optimization.TestSuite1/sampleSkippedTest",
      "java:suite://com.example.optimization.TestSuite2",
      "java:test://com.example.optimization.TestSuite2/test1",
      "java:test://com.example.optimization.TestSuite2/test2",
      "java:test://com.example.optimization.TestSuite2/test3",
      "java:test://com.example.optimization.TestSuite2/test4",
      "java:test://com.example.optimization.TestSuite2/test5",
      "java:suite://com.example.optimization.TestSuite3",
      "java:test://com.example.optimization.TestSuite3/test1",
      "java:suite://com.example.optimization.TestSuite4",
      "java:test://com.example.optimization.TestSuite4/test1",
    )
    client.testNodes.size shouldBe 17 // one of the test suites in the XML is empty, so we hide it
    client.testNodes.map { it.locationUrl } shouldBe expectedTestHints

    val expectedNames =
      listOf(
        "com.example.optimization",
        "TestSuite1",
        "test1",
        "test2",
        "test3",
        "test4",
        "test5",
        "TestSuite2",
        "test1",
        "sampleFailedTest",
        "test3",
        "test4",
        "sampleSkippedTest",
        "TestSuite3",
        "test1",
        "TestSuite4",
        "test1",
      )

    client.testNodes.map { it.name } shouldContainExactlyInAnyOrder expectedNames

    client.testNodes.map {
      val data = it
      when (data.name) {
        "com.example.optimization",
        "TestSuite1" -> {
          data.isSuite shouldBe true
        }

        "sampleFailedTest" -> {
          data.isDefect shouldBe true
          data.errorMessage shouldContain "expected:"
          data.output() shouldNotBe ""
        }

        "sampleSkippedTest" -> {
          data.isIgnored shouldBe true
        }

        else -> {
          if (!data.isSuite) data.isPassed shouldBe true
        }
      }
    }
  }

  @Test
  fun `junit5 - success, failure and ignored (ANSI colors)`(
    @TempDir tempDir: Path,
  ) {
    val dollar = "${'$'}"
    val sampleContents =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <testsuites>
        <testsuite name="src/test/kotlin/org/jetbrains/simple/TripleTest" tests="1" failures="0" errors="1">
          <testcase name="src/test/kotlin/org/jetbrains/simple/TripleTest" status="run" duration="0" time="0"><error message="exited with error code 1"></error></testcase>
            <system-out>
      Generated test.log (if the file is not UTF-8, then this may be unreadable):
      <![CDATA[exec $dollar{PAGER:-/ usr / bin / less} "$dollar{'$dollar'}0" || exit 1
      Executing tests from //src/test/kotlin/org/jetbrains/simple:TripleTest
      -----------------------------------------------------------------------------

      Thanks for using JUnit! Support its development at https://junit.org/sponsoring

      ?[36m╷?[0m
      ?[36m└─?[0m ?[36mJUnit Jupiter?[0m ?[32m✔?[0m
      ?[36m   └─?[0m ?[36mTripleTest?[0m ?[32m✔?[0m
      ?[36m      ├─?[0m ?[31mtestFailure()?[0m ?[31m✘?[0m ?[31mThis test always fails?[0m
      ?[36m      ├─?[0m ?[35mtestIgnored()?[0m ?[35m↷?[0m ?[35mpublic final void org.jetbrains.simple.TripleTest.testIgnored() is @Disabled?[0m
      ?[36m      └─?[0m ?[34mtestSuccess()?[0m ?[32m✔?[0m

      Failures (1):
        JUnit Jupiter:TripleTest:testFailure()
          MethodSource [className = 'org.jetbrains.simple.TripleTest', methodName = 'testFailure', methodParameterTypes = '']
          => java.lang.AssertionError: This test always fails
             org.jetbrains.simple.TripleTest.testFailure(TripleTest.kt:11)
             java.base/java.lang.reflect.Method.invoke(Method.java:580)
             java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
             java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

      Test run finished after 53 ms
      [         2 containers found      ]
      [         0 containers skipped    ]
      [         2 containers started    ]
      [         0 containers aborted    ]
      [         2 containers successful ]
      [         0 containers failed     ]
      [         3 tests found           ]
      [         1 tests skipped         ]
      [         2 tests started         ]
      [         0 tests aborted         ]
      [         1 tests successful      ]
      [         1 tests failed          ]


      WARNING: Delegated to the 'execute' command.
               This behaviour has been deprecated and will be removed in a future release.
               Please use the 'execute' command directly.]]>
            </system-out>
          </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    // then
    client.testNodes.size shouldBe 4

    val expectedNames =
      listOf(
        "TripleTest",
        "testFailure()",
        "testIgnored()",
        "testSuccess()",
      )

    val testFinishes = client.testNodes

    testFinishes.map { it.name } shouldContainExactlyInAnyOrder expectedNames

    testFinishes.forEach {
      val data = it
      when (data.name) {
        "TripleTest" -> {
        }
        "testFailure()" -> {
          data.isDefect shouldBe true
          data.errorMessage shouldNotBe null
          data.errorMessage!!.split("\n".toRegex()).size shouldBe 7
        }
        "testIgnored()" -> {
          data.isIgnored shouldBe true
        }
        "testSuccess()" -> {
          data.isPassed shouldBe true
        }
      }
    }
  }

  @Test
  fun `junit5 - success, failure and ignored (no ANSI colors)`(
    @TempDir tempDir: Path,
  ) {
    val dollar = "${'$'}"
    val sampleContents =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <testsuites>
        <testsuite name="src/test/kotlin/org/jetbrains/simple/TripleTest" tests="1" failures="0" errors="1">
          <testcase name="src/test/kotlin/org/jetbrains/simple/TripleTest" status="run" duration="0" time="0"><error message="exited with error code 1"></error></testcase>
            <system-out>
      Generated test.log (if the file is not UTF-8, then this may be unreadable):
      <![CDATA[exec $dollar{PAGER:-/ usr / bin / less} "$dollar{'$dollar'}0" || exit 1
      Executing tests from //src/test/kotlin/org/jetbrains/simple:TripleTest
      -----------------------------------------------------------------------------

      Thanks for using JUnit! Support its development at https://junit.org/sponsoring

      ╷
      └─ JUnit Jupiter ✔
         └─ TripleTest ✔
            ├─ testFailure() ✘ This test always fails
            ├─ testIgnored() ↷ public final void org.jetbrains.simple.TripleTest.testIgnored() is @Disabled
            └─ testSuccess() ✔

      Failures (1):
        JUnit Jupiter:TripleTest:testFailure()
          MethodSource [className = 'org.jetbrains.simple.TripleTest', methodName = 'testFailure', methodParameterTypes = '']
          => java.lang.AssertionError: This test always fails
             org.jetbrains.simple.TripleTest.testFailure(TripleTest.kt:11)
             java.base/java.lang.reflect.Method.invoke(Method.java:580)
             java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
             java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

      Test run finished after 53 ms
      [         2 containers found      ]
      [         0 containers skipped    ]
      [         2 containers started    ]
      [         0 containers aborted    ]
      [         2 containers successful ]
      [         0 containers failed     ]
      [         3 tests found           ]
      [         1 tests skipped         ]
      [         2 tests started         ]
      [         0 tests aborted         ]
      [         1 tests successful      ]
      [         1 tests failed          ]


      WARNING: Delegated to the 'execute' command.
               This behaviour has been deprecated and will be removed in a future release.
               Please use the 'execute' command directly.]]>
            </system-out>
          </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    // then
    client.testNodes.size shouldBe 4

    val expectedNames =
      listOf(
        "TripleTest",
        "testFailure()",
        "testIgnored()",
        "testSuccess()",
      )

    val testFinishes = client.testNodes

    testFinishes.map { it.name } shouldContainExactlyInAnyOrder expectedNames
    testFinishes.forEach {
      val data = it
      when (data.name) {
        "TripleTest" -> {
        }
        "testFailure()" -> {
          data.isDefect shouldBe true
          data.errorMessage shouldNotBe null
          data.errorMessage!!.split("\n".toRegex()).size shouldBe 7
        }
        "testIgnored()" -> {
          data.isIgnored shouldBe true
        }
        "testSuccess()" -> {
          data.isPassed shouldBe true
        }
      }
    }
  }

  @Test
  fun `junit5 - malformed`(
    @TempDir tempDir: Path,
  ) {
    val dollar = "${'$'}"
    val sampleContents =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <testsuites>
        <testsuite name="src/test/kotlin/org/jetbrains/simple/TripleTest" tests="1" failures="0" errors="1">
          <testcase name="src/test/kotlin/org/jetbrains/simple/TripleTest" status="run" duration="0" time="0"><error message="exited with error code 1"></error></testcase>
            <system-out>
      Generated test.log (if the file is not UTF-8, then this may be unreadable):
      <![CDATA[exec $dollar{PAGER:-/ usr / bin / less} "$dollar{'$dollar'}0" || exit 1
      Executing tests from //src/test/kotlin/org/jetbrains/simple:TripleTest
      -----------------------------------------------------------------------------

      Thanks for using JUnit! Support its development at https://junit.org/sponsoring

      ╷
      └─ JUnit Jupiter ✔
         └!─ TripleTest ✔
            ─ testFailure() ✘ This test always fails
            ├─ testIgnored() ↷ public final void org.jetbrains.simple.TripleTest.testIgnored() is @Disabled
            └─ testSuccess() ✔

      Failures (1):
        JUnit Jupiter:TripleTest:testFailure()
          MethodSource [className = 'org.jetbrains.simple.TripleTest', methodName = 'testFailure', methodParameterTypes = '']
          => java.lang.AssertionError: This test always fails
             org.jetbrains.simple.TripleTest.testFailure(TripleTest.kt:11)
             java.base/java.lang.reflect.Method.invoke(Method.java:580)
             java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
             java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

      Test run finished after 53 ms
      [         2 containers found      ]
      [         0 containers skipped    ]
      [         2 containers started    ]
      [         0 containers aborted    ]
      [         2 containers successful ]
      [         0 containers failed     ]
      [         3 tests found           ]
      [         1 tests skipped         ]
      [         2 tests started         ]
      [         0 tests aborted         ]
      [         1 tests successful      ]
      [         1 tests failed          ]


      WARNING: Delegated to the 'execute' command.
               This behaviour has been deprecated and will be removed in a future release.
               Please use the 'execute' command directly.]]>
            </system-out>
          </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    // then
    client.testNodes.size shouldBe 2 // two malformed rows should simply be ignored

    val expectedNames = // two malformed rows should simply be ignored
      listOf(
        "testIgnored()",
        "testSuccess()",
      )

    val testFinishes = client.testNodes
    testFinishes.size shouldBe 2 // two malformed rows should simply be ignored

    testFinishes.map { it.name } shouldContainExactlyInAnyOrder expectedNames

    testFinishes.forEach {
      val data = it
      when (data.name) {
        "testIgnored()" -> {
          data.isIgnored shouldBe true
        }
        "testSuccess()" -> {
          data.isPassed shouldBe true
        }
      }
    }
  }

  @Test
  fun `junit5 - all failure, multiple testcase`(
    @TempDir tempDir: Path,
  ) {
    val sampleContents =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <testsuites>
          <testsuite name="testsuite" tests="2" failures="0" errors="2" disabled="0" skipped="0" package="">
              <properties/>
              <testcase name="testFailure1" classname="org.jetbrains.simple.DoubleFailTest" time="0.02">
                  <error message="first fail ==&gt; expected: &lt;true&gt; but was: &lt;false&gt;" type="org.opentest4j.AssertionFailedError">
                      <![CDATA[org.opentest4j.AssertionFailedError: first fail ==> expected: <true> but was: <false>
                      	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
                      	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
                      	at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
                      	at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
                      	at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:210)
                      	at org.jetbrains.simple.DoubleFailTest.testFailure1(DoubleFailTest.java:44)
                      	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                      	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                      	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                      ]]>
                  </error>
              </testcase>
              <testcase name="testFailure2" classname="org.jetbrains.simple.DoubleFailTest" time="0">
                  <error message="second fail ==&gt; expected: &lt;true&gt; but was: &lt;false&gt;" type="org.opentest4j.AssertionFailedError">
                      <![CDATA[org.opentest4j.AssertionFailedError: second fail ==> expected: <true> but was: <false>
                      	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
                      	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
                      	at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
                      	at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
                      	at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:210)
                      	at org.jetbrains.simple.DoubleFailTest.testFailure2(DoubleFailTest.java:49)
                      	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                      	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                      	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                      ]]>
                  </error>
              </testcase>
          </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    // then
    client.testNodes.size shouldBe 3

    val expectedNames = listOf("testsuite", "testFailure1", "testFailure2")

    client.testNodes.map { it.name } shouldContainExactlyInAnyOrder expectedNames
  }

  @Test
  fun `junit5 - incomplete test report time`(
    @TempDir tempDir: Path,
  ) {
    val sampleContents =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <testsuites>
          <testsuite name="testsuite" tests="1" failures="0" errors="1" disabled="0" skipped="0" package="">
              <properties/>
              <testcase name="testFailure1" classname="org.jetbrains.simple.DoubleFailTest" time="0.02">
                  <error message="first fail ==&gt; expected: &lt;true&gt; but was: &lt;false&gt;" type="org.opentest4j.AssertionFailedError">
                      <![CDATA[org.opentest4j.AssertionFailedError: first fail ==> expected: <true> but was: <false>
                      	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
                      	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
                      	at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
                      	at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
                      	at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:210)
                      	at org.jetbrains.simple.DoubleFailTest.testFailure1(DoubleFailTest.java:44)
                      	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                      	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                      	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                      ]]>
                  </error>
              </testcase>
          </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    // then
    client.testNodes.single { it.name == "testFailure1" }.duration shouldBe 20L
  }

  /**
   * ```kotlin
   * import org.junit.jupiter.api.Test
   *
   * class MultilineExceptionTest {
   *   @Test
   *   fun `throws multi-line exception`() {
   *     throw Exception("First line\n  Second line\nThird line")
   *   }
   *
   *   @Test
   *   fun `throws single-line exception with trailing newline`() {
   *     throw Exception("First line\n")
   *   }
   * }
   * ```
   */
  @Test
  fun `junit5 - parsing multiline error messages`(
    @TempDir tempDir: Path,
  ) {
    val dollar = "${'$'}"
    val sampleContents =
      """
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="server/server/src/test/kotlin/org/jetbrains/bazel/server/diagnostics/MultilineExceptionTest" tests="1" failures="0" errors="1">
    <testcase name="server/server/src/test/kotlin/org/jetbrains/bazel/server/diagnostics/MultilineExceptionTest" status="run" duration="0" time="0"><error message="exited with error code 1"></error></testcase>
      <system-out>
Generated test.log (if the file is not UTF-8, then this may be unreadable):
<![CDATA[exec $dollar{PAGER:-/usr/bin/less} "$dollar{'$dollar'}0" || exit 1
Executing tests from //server/server/src/test/kotlin/org/jetbrains/bazel/server/diagnostics:MultilineExceptionTest
-----------------------------------------------------------------------------

Thanks for using JUnit! Support its development at https://junit.org/sponsoring

?[36m╷?[0m
?[36m└─?[0m ?[36mJUnit Platform Suite?[0m ?[32m✔?[0m
?[36m   └─?[0m ?[36mMultilineExceptionTest?[0m ?[32m✔?[0m
?[36m      ├─?[0m ?[31mthrows multi-line exception()?[0m ?[31m✘?[0m ?[31mFirst line?[0m
?[36m      │     ?[0m?[31m     Second line?[0m
?[36m      │     ?[0m?[31m   Third line?[0m
?[36m      └─?[0m ?[31mthrows single-line exception with trailing newline()?[0m ?[31m✘?[0m ?[31mFirst line?[0m

Failures (2):
  JUnit Platform Suite:MultilineExceptionTest:throws multi-line exception()
    MethodSource [className = 'org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest', methodName = 'throws multi-line exception', methodParameterTypes = '']
    => java.lang.Exception: First line
  Second line
Third line
       org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest.throws multi-line exception(MultilineExceptionTest.kt:8)
       java.base/java.lang.reflect.Method.invoke(Method.java:568)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
  JUnit Platform Suite:MultilineExceptionTest:throws single-line exception with trailing newline()
    MethodSource [className = 'org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest', methodName = 'throws single-line exception with trailing newline', methodParameterTypes = '']
    => java.lang.Exception: First line

       org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest.throws single-line exception with trailing newline(MultilineExceptionTest.kt:13)
       java.base/java.lang.reflect.Method.invoke(Method.java:568)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1511)

Test run finished after 36 ms
[         2 containers found      ]
[         0 containers skipped    ]
[         2 containers started    ]
[         0 containers aborted    ]
[         2 containers successful ]
[         0 containers failed     ]
[         2 tests found           ]
[         0 tests skipped         ]
[         2 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         2 tests failed          ]


WARNING: Delegated to the 'execute' command.
         This behaviour has been deprecated and will be removed in a future release.
         Please use the 'execute' command directly.]]>
      </system-out>
    </testsuite>
</testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    // then
    val expectedNames =
      listOf(
        "MultilineExceptionTest",
        "throws multi-line exception()",
        "throws single-line exception with trailing newline()",
      )

    val testFinishes = client.testNodes

    testFinishes.map { it.name } shouldContainExactlyInAnyOrder expectedNames

    testFinishes.forEach {
      val data = it
      when (data.name) {
        "throws multi-line exception()" -> {
          data.isDefect shouldBe true
          data.errorMessage shouldBe
            """
            First line
              Second line
            Third line
                MethodSource [className = 'org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest', methodName = 'throws multi-line exception', methodParameterTypes = '']
            java.lang.Exception: First line
              Second line
            Third line
                   org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest.throws multi-line exception(MultilineExceptionTest.kt:8)
                   java.base/java.lang.reflect.Method.invoke(Method.java:568)
                   java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
                   java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
            """.trimIndent()
        }
        "throws single-line exception with trailing newline()" -> {
          data.isDefect shouldBe true
          data.errorMessage shouldBe
            """
            First line
                MethodSource [className = 'org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest', methodName = 'throws single-line exception with trailing newline', methodParameterTypes = '']
            java.lang.Exception: First line

                   org.jetbrains.bazel.server.diagnostics.MultilineExceptionTest.throws single-line exception with trailing newline(MultilineExceptionTest.kt:13)
                   java.base/java.lang.reflect.Method.invoke(Method.java:568)
                   java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
                   java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
            """.trimIndent()
        }
      }
    }
  }

  @Test
  fun `junit4 - output message and correct test status`(@TempDir tempDir: Path) {
    val sampleContents = """
<?xml version='1.0' encoding='UTF-8'?><testsuites><testsuite name="com.example.MyJunit4Test" tests="6" failures="0" errors="1" disabled="0" skipped="0" package=""><properties/><testcase name="testError" classname="com.example.MyJunit4Test" time="0.01"><error message="expected: &lt;mock label> but was: &lt;mock label1>" type="org.opentest4j.AssertionFailedError"><![CDATA[org.opentest4j.AssertionFailedError: expected: <mock label> but was: <mock label1>
	at org.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:55)
	at org.junit.jupiter.api.AssertionUtils.failNotEqual(AssertionUtils.java:62)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1141)
]]></error></testcase><testcase name="testSystemOut" classname="com.example.MyJunit4Test" time="0.01"><system-out><![CDATA[Hi from test!
]]></system-out></testcase><testcase name="testAnother" classname="com.example.MyJunit4Test" time="0.01"/></testsuite></testsuites>
    """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    // when
    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    // then
    client.testNodes.size shouldBe 4

    val expectedNames =
      listOf(
        "com.example.MyJunit4Test",
        "testError",
        "testSystemOut",
        "testAnother",
      )

    val testFinishes = client.testNodes

    testFinishes.map { it.name } shouldContainExactlyInAnyOrder expectedNames

    testFinishes.forEach {
      val data = it
      when (data.name) {
        "com.example.MyJunit4Test" -> {
        }

        "testError" -> {
          data.isDefect shouldBe true
          data.errorMessage shouldBe "expected: <mock label> but was: <mock label1>"
          data.output() shouldContain "at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1141)"
        }

        "testSystemOut" -> {
          data.isPassed shouldBe true
          data.output() shouldBe "Hi from test!\n"
        }

        "testAnother" -> {
          data.isPassed shouldBe true
        }
      }
    }
  }

  @Test
  fun `empty testsuites element reports nothing`(
    @TempDir tempDir: Path,
  ) {
    // Go tests via rules_go without verbose output produce an empty <testsuites> on success.
    // Nothing is reported, so parseAndReport must return false — BepServer then shows a target-level node
    // instead of leaving the user with "No tests were found".
    val sampleContents = "<testsuites></testsuites>"

    val client = MockBuildTaskEventsHandler()

    val reported =
      TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    reported shouldBe false
    client.testNodes.size shouldBe 0
  }

  @Test
  fun `suite with tests but no testcases is skipped`(
    @TempDir tempDir: Path,
  ) {
    // A well-formed suite with tests > 0 but no <testcase> children is skipped (aggregation / filtered
    // suites). parseAndReport returns false so the caller can fall back to a target-level result.
    val sampleContents =
      """
      <?xml version='1.0' encoding='UTF-8'?>
      <testsuites>
        <testsuite name='//src/go/mypackage:mypackage_test' timestamp='2026-01-15T10:00:00Z' hostname='localhost' tests='3' failures='0' errors='0' time='1.234' package='' id='0'>
          <properties />
          <system-out />
          <system-err />
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    val reported = TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), null)

    reported shouldBe false
    client.testNodes.size shouldBe 0
  }

  @Test
  fun `passing suite with testcases is reported`(
    @TempDir tempDir: Path,
  ) {
    val sampleContents =
      """
      <?xml version='1.0' encoding='UTF-8'?>
      <testsuites>
        <testsuite name='//src/go/passing:passing_test' timestamp='2026-01-15T10:00:00Z' hostname='localhost' tests='1' failures='0' errors='0' time='0.5' package='' id='0'>
          <testcase name='TestSomething' classname='passing_test' time='0.5' />
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    val reported =
      TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), pythonTargetKind)

    reported shouldBe true
    val expectedNames = listOf("//src/go/passing:passing_test", "TestSomething")
    val finishes = client.testNodes
    finishes.map { it.name } shouldContainExactlyInAnyOrder expectedNames
  }

  @Test
  fun `fallback parser path reports suites`(
    @TempDir tempDir: Path,
  ) {
    // Missing the `id` attribute makes the strict primary parser fail, so this goes through the
    // lenient fallback parser. parseAndReport must still return true so the caller does
    // not wrongly emit a target-level fallback on top of the per-test results.
    val sampleContents =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <testsuites>
        <testsuite name="test_suite" errors="0" failures="0" skipped="0" tests="1" time="0.01" timestamp="2024-05-17T19:23:31" hostname="test-host">
          <testcase classname="com.example.Test" name="test_method" time="0.01" />
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    val reported = TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), null)

    reported shouldBe true
    client.testNodes.size shouldBe 2
  }

  @Test
  fun `suite names split at dots and collapse nodes with one child`(
    @TempDir tempDir: Path,
  ) {
    val sampleContents =
      """
      <?xml version='1.0' encoding='UTF-8'?>
      <testsuites>
        <testsuite name='com.example.a.FooTest' tests='1' failures='0' errors='0' time='0.1' package='' id='0'>
          <testcase name='test1' classname='com.example.a.FooTest' time='0.1' />
        </testsuite>
        <testsuite name='com.example.b.BarTest' tests='1' failures='0' errors='0' time='0.1' package='' id='1'>
          <testcase name='test1' classname='com.example.b.BarTest' time='0.1' />
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), javaTargetKind)

    client.testNodes.map { "${it.parent.name}/${it.name}" } shouldBe listOf(
      "[root]/com.example",
      "com.example/a.FooTest",
      "a.FooTest/test1",
      "com.example/b.BarTest",
      "b.BarTest/test1",
    )
    client.testNodes.map { it.locationUrl } shouldBe listOf(
      null,
      "java:suite://com.example.a.FooTest",
      "java:test://com.example.a.FooTest/test1",
      "java:suite://com.example.b.BarTest",
      "java:test://com.example.b.BarTest/test1",
    )
  }

  @Test
  fun `go suites share the package node and hold their subtests`(
    @TempDir tempDir: Path,
  ) {
    val sampleContents =
      """
      <testsuites>
        <testsuite errors="0" failures="0" skipped="0" tests="1" time="0.001" name="server/filter/filter_test.TestSingle">
          <testcase classname="filter_test" name="TestSingle" time="0.000"></testcase>
        </testsuite>
        <testsuite errors="0" failures="1" skipped="0" tests="3" time="0.630" name="server/filter/filter_test.TestWithSubtests">
          <testcase classname="filter_test" name="TestWithSubtests" time="0.630"></testcase>
          <testcase classname="filter_test" name="TestWithSubtests/Passes" time="0.030"></testcase>
          <testcase classname="filter_test" name="TestWithSubtests/Fails" time="0.030"><failure message="Failed" type="">filter_test.go:10: wrong</failure></testcase>
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), null)

    client.testNodes.map { "${it.parent.name}/${it.name}" } shouldBe listOf(
      "[root]/server/filter/filter_test",
      "server/filter/filter_test/TestSingle",
      "server/filter/filter_test/TestWithSubtests",
      "TestWithSubtests/TestWithSubtests",
      "TestWithSubtests/Passes",
      "TestWithSubtests/Fails",
    )
    client.testNodes.single { it.name == "Fails" }.isDefect shouldBe true
  }

  @Test
  fun `suites and test cases with the same name are all reported`(
    @TempDir tempDir: Path,
  ) {
    val sampleContents =
      """
      <testsuites>
        <testsuite name="catch" tests="2" failures="1" errors="0" time="0.1">
          <testcase classname="catch.global" name="Generated" time="0.1"><failure message="failed run" type="CHECK">TestBase.cpp:10</failure></testcase>
          <testcase classname="catch.global" name="Generated" time="0.1"></testcase>
        </testsuite>
        <testsuite name="catch" tests="1" failures="1" errors="0" time="0.1">
          <testcase classname="catch.global" name="Other" time="0.1"><failure message="failed" type="CHECK">TestBase.cpp:20</failure></testcase>
        </testsuite>
      </testsuites>
      """.trimIndent()

    val client = MockBuildTaskEventsHandler()

    TestXmlParser(client).parseAndReport(TaskGroupId.EMPTY.task(""), writeTempFile(tempDir, sampleContents), null)

    client.testNodes.map { "${it.name}: ${it.isDefect}" } shouldBe listOf("catch: true", "Generated: true", "Generated: false", "Other: true")
  }

  private fun writeTempFile(tempDir: Path, contents: String): Path {
    val tempFile = tempDir.resolve("tempFile.xml")
    tempFile.writeText(contents)
    return tempFile
  }
}
