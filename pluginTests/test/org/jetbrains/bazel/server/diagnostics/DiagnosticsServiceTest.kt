package org.jetbrains.bazel.server.diagnostics

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.Position
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.createTempFile

class DiagnosticsServiceTest {
  private val workspacePath = Paths.get("/user/workspace")

  @Test
  fun `should extract diagnostics for error in BUILD file`() {
    val output =
      """
      Loading:
      Loading: 0 packages loaded
      Analyzing: target //path/to/package:test (0 packages loaded, 0 targets configured)
      INFO: Analyzed target //path/to/package:test (0 packages loaded, 0 targets configured).
      INFO: Found 1 target...
      [0 / 3] [Prepa] BazelWorkspaceStatusAction stable-status.txt
      ERROR: /user/workspace/path/to/package/BUILD:12:37: in java_test rule //path/to/package:test: target '//path/to/another/package:lib' is not visible from target '//path/to/package:test'. Check the visibility declaration of the former target if you think the dependency is legitimate
      """.trimIndent()

    // when
    val diagnostics = extractDiagnostics(output, Label.parse("//path/to/package:test"), isCommandLineFormattedOutput = true)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/path/to/package/BUILD")),
          Label.parse("@//path/to/package:test"),
          errorDiagnostic(
            Position.fromHumanReadable(12, 37),
            "in java_test rule //path/to/package:test: target '//path/to/another/package:lib' is not visible from target '//path/to/package:test'. Check the visibility declaration of the former target if you think the dependency is legitimate",
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should extract diagnostics for error in one source file`() {
    val output =
      """
            |path/to/package/Test.scala:3: error: type mismatch;
            | found   : String("test")
            | required: Int
            |  val foo: Int = "test"
            |                 ^
            |one error found
            |Build failed
            |java.lang.RuntimeException: Build failed
            |  at io.bazel.rulesscala.scalac.ScalacWorker.compileScalaSources(ScalacWorker.java:280)
            |  at io.bazel.rulesscala.scalac.ScalacWorker.work(ScalacWorker.java:63)
            |  at io.bazel.rulesscala.worker.Worker.persistentWorkerMain(Worker.java:92)
            |  at io.bazel.rulesscala.worker.Worker.workerMain(Worker.java:46)
            |  at io.bazel.rulesscala.scalac.ScalacWorker.main(ScalacWorker.java:26)
      """.trimMargin()

    // when
    val diagnostics = extractDiagnostics(output, Label.parse("//path/to/package:test"))

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/path/to/package/Test.scala")),
          Label.parse("@//path/to/package:test"),
          errorDiagnostic(
            Position.fromHumanReadable(3, 18),
            """type mismatch;
                  | found   : String("test")
                  | required: Int
                  |  val foo: Int = "test"
                  |                 ^
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should extract diagnostics for error in 2 source files`() {
    val output =
      """
            |path/to/package/Test1.scala:21: error: type mismatch;
            |  found   : Int(42)
            |  required: String
            |    val x: String = 42
            |                    ^
            |path/to/package/Test2.scala:37: error: type mismatch;
            |  found   : String("test")
            |  required: Int
            |    val x: Int = "test"
            |                 ^
            |2 errors
            |Build failed
            |java.lang.RuntimeException: Build failed
            |  at io.bazel.rulesscala.scalac.ScalacWorker.compileScalaSources(ScalacWorker.java:280)
            |  at io.bazel.rulesscala.scalac.ScalacWorker.work(ScalacWorker.java:63)
            |  at io.bazel.rulesscala.worker.Worker.persistentWorkerMain(Worker.java:92)
            |  at io.bazel.rulesscala.worker.Worker.workerMain(Worker.java:46)
            |  at io.bazel.rulesscala.scalac.ScalacWorker.main(ScalacWorker.java:26)
      """.trimMargin()

    // when
    val diagnostics = extractDiagnostics(output, Label.parse("//path/to/package:test"))

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/path/to/package/Test1.scala")),
          Label.parse("@//path/to/package:test"),
          errorDiagnostic(
            Position.fromHumanReadable(21, 21),
            """type mismatch;
                  |  found   : Int(42)
                  |  required: String
                  |    val x: String = 42
                  |                    ^
            """.trimMargin(),
          ),
        ),
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/path/to/package/Test2.scala")),
          Label.parse("@//path/to/package:test"),
          errorDiagnostic(
            Position.fromHumanReadable(37, 18),
            """type mismatch;
                  |  found   : String("test")
                  |  required: Int
                  |    val x: Int = "test"
                  |                 ^
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse errors from color formatted command line output`() {
    // given
    val tmpFile = createTempFile().toAbsolutePath()
    val output =
      """
        |[31m[1mERROR: [0mmodule extension @@googleapis+//:extensions.bzl%switched_rules does not generate repository "com_google_googleapis_imports", yet it is imported as "com_google_googleapis_imports" in the usage at https://bcr.bazel.build/modules/grpc/1.66.0.bcr.2/MODULE.bazel:39:31
        |[31m[1mERROR: [0mResults may be incomplete as 1 extension failed.
      """.trimMargin()

    // when
    val label = Label.parse("//plugin-bazel/src/test/java/org/jetbrains/bazel/server/diagnostics:diagnostics")
    val diagnostics = extractDiagnostics(output, label, isCommandLineFormattedOutput = true)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          textDocument = null,
          buildTarget = label,
          errorDiagnostic(
            Position.NONE,
            "module extension @@googleapis+//:extensions.bzl%switched_rules does not generate repository \"com_google_googleapis_imports\", yet it is imported as \"com_google_googleapis_imports\" in the usage at https://bcr.bazel.build/modules/grpc/1.66.0.bcr.2/MODULE.bazel:39:31",
          ),
          errorDiagnostic(
            Position.NONE,
            "Results may be incomplete as 1 extension failed.",
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should extract diagnostics for 2 errors in 1 source file`() {
    val output =
      """
            |path/to/package/Test.scala:21: error: type mismatch;
            |  found   : Int(42)
            |  required: String
            |    val x: String = 42
            |                    ^
            |path/to/package/Test.scala:37: error: type mismatch;
            |  found   : String("test")
            |  required: Int
            |    val x: Int = "test"
            |                 ^
            |2 errors
            |Build failed
            |java.lang.RuntimeException: Build failed
            |  at io.bazel.rulesscala.scalac.ScalacWorker.compileScalaSources(ScalacWorker.java:280)
            |  at io.bazel.rulesscala.scalac.ScalacWorker.work(ScalacWorker.java:63)
            |  at io.bazel.rulesscala.worker.Worker.persistentWorkerMain(Worker.java:92)
            |  at io.bazel.rulesscala.worker.Worker.workerMain(Worker.java:46)
            |  at io.bazel.rulesscala.scalac.ScalacWorker.main(ScalacWorker.java:26)
      """.trimMargin()
    // when
    val diagnostics = extractDiagnostics(output, Label.parse("//path/to/package:test"))

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/path/to/package/Test.scala")),
          Label.parse("@//path/to/package:test"),
          errorDiagnostic(
            Position.fromHumanReadable(21, 21),
            """type mismatch;
                  |  found   : Int(42)
                  |  required: String
                  |    val x: String = 42
                  |                    ^
            """.trimMargin(),
          ),
          errorDiagnostic(
            Position.fromHumanReadable(37, 18),
            """type mismatch;
                  |  found   : String("test")
                  |  required: Int
                  |    val x: Int = "test"
                  |                 ^
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should parse kotlin errors`() {
    // given
    val output =
      """
      server/src/test/java/org/jetbrains/bazel/server/diagnostics/DiagnosticsServiceTest.kt:12:18: error: type mismatch: inferred type is String but Int was expected
      val int: Int = "STRING"
      ^
      server/src/test/java/org/jetbrains/bazel/server/diagnostics/DiagnosticsServiceTest.kt:13:24: error: the integer literal does not conform to the expected type String
      val string: String = 1
      ^
      kwi 13, 2022 12:07:04 PM worker request 0
      SEVERE: Compilation failure: compile phase failed:
      server/src/test/java/org/jetbrains/bazel/server/diagnostics/DiagnosticsServiceTest.kt:12:18: error: type mismatch: inferred type is String but Int was expected
      val int: Int = "STRING"
      ^
      server/src/test/java/org/jetbrains/bazel/server/diagnostics/DiagnosticsServiceTest.kt:13:24: error: the integer literal does not conform to the expected type String
      val string: String = 1
      ^
      """.trimIndent()

    // when
    val label = Label.parse("//plugin-bazel/src/test/java/org/jetbrains/bazel/server/diagnostics:diagnostics")

    val diagnostics =
      extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(
            Path("/user/workspace/server/src/test/java/org/jetbrains/bazel/server/diagnostics/DiagnosticsServiceTest.kt"),
          ),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(12, 18),
            """type mismatch: inferred type is String but Int was expected
                  |val int: Int = "STRING"
                  |^
            """.trimMargin(),
          ),
          errorDiagnostic(
            Position.fromHumanReadable(13, 24),
            """the integer literal does not conform to the expected type String
                  |val string: String = 1
                  |^
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse scala errors with warnings`() {
    // given
    val output =
      """          |project/src/main/scala/com/example/project/File1.scala:11: error: type mismatch;
                   |  found   : String("sd")
                   |  required: Int
                   |    val x: Int = "sd"
                   |                 ^
                   |project/src/main/scala/com/example/project/File1.scala:11: warning: local val x in method promote is never used
                   |  val x: Int = "sd"
                   |      ^
                   |project/src/main/scala/com/example/project/File2.scala:26: warning: private val versionsWriter in object File2 is never used
                   |  private implicit val versionsWriter: ConfigWriter[Versions] = deriveWriter[Versions]
                   |                       ^
                   |project/src/main/scala/com/example/project/File2.scala:28: warning: private val File2ProtocolWriter in object File2 is never used
                   |private implicit val File2ProtocolWriter: ConfigWriter[File2Protocol] =
                   |                     ^
                   |three warnings found
                   |one error found
      """.trimMargin()

    // when
    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/project/src/main/scala/com/example/project/File1.scala")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(11, 18),
            """type mismatch;
                  |  found   : String("sd")
                  |  required: Int
                  |    val x: Int = "sd"
                  |                 ^
            """.trimMargin(),
          ),
          warningDiagnostic(
            Position.fromHumanReadable(11, 7),
            """local val x in method promote is never used
                   |  val x: Int = "sd"
                   |      ^
            """.trimMargin(),
          ),
        ),
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/project/src/main/scala/com/example/project/File2.scala")),
          Label.parse(label.toString()),
          warningDiagnostic(
            Position.fromHumanReadable(26, 24),
            """private val versionsWriter in object File2 is never used
                   |  private implicit val versionsWriter: ConfigWriter[Versions] = deriveWriter[Versions]
                   |                       ^
            """.trimMargin(),
          ),
          warningDiagnostic(
            Position.fromHumanReadable(28, 22),
            """private val File2ProtocolWriter in object File2 is never used
                   |private implicit val File2ProtocolWriter: ConfigWriter[File2Protocol] =
                   |                     ^
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse scala 3 error`() {
    // given
    val output =
      """
            |[31m[31m-- [E007] Type Mismatch Error: Hello.scala:19:20 -------------------------------[0m[0m
            |[31m19 |[0m  [33mdef[0m [36mhello4[0m: [35mInt[0m = [31m"Hello"[0m
            |[31m[31m   |[0m                    ^^^^^^^[0m
            |[31m   |[0m                    Found:    ([32m"Hello"[0m : String)
            |[31m   |[0m                    Required: Int
            |[31m   |[0m
            |[31m   |[0m longer explanation available when compiling with `-explain`
            |2 errors found
            |Build failed
            |java.lang.RuntimeException: Build failed
            |	at io.bazel.rulesscala.scalac.ScalacInvoker.invokeCompiler(ScalacInvoker3.java:43)
            |	at io.bazel.rulesscala.scalac.ScalacWorker.compileScalaSources(ScalacWorker.java:253)
            |	at io.bazel.rulesscala.scalac.ScalacWorker.work(ScalacWorker.java:69)
            |	at io.bazel.rulesscala.worker.Worker.persistentWorkerMain(Worker.java:86)
            |	at io.bazel.rulesscala.worker.Worker.workerMain(Worker.java:39)
            |   at io.bazel.rulesscala.scalac.ScalacWorker.main(ScalacWorker.java:33)
      """.trimMargin()

    // when
    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/Hello.scala")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(19, 20),
            """|Type Mismatch Error
                       |Found:    ("Hello" : String)
                       |Required: Int
                       |
                       |longer explanation available when compiling with `-explain`
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse scala 3 multiple errors`() {
    // given
    val output =
      """
            |[31m[31m-- [E007] Type Mismatch Error: Hello.scala:18:20 -------------------------------[0m[0m
            |[31m18 |[0m  [33mdef[0m [36mhello3[0m: [35mInt[0m = [31m"Hello"[0m
            |[31m[31m   |[0m                    ^^^^^^^[0m
            |[31m   |[0m                    Found:    ([32m"Hello"[0m : String)
            |[31m   |[0m                    Required: Int
            |[31m   |[0m
            |[31m   |[0m longer explanation available when compiling with `-explain`
            |[31m[31m-- [E007] Type Mismatch Error: Hello.scala:19:20 -------------------------------[0m[0m
            |[31m19 |[0m  [33mdef[0m [36mhello4[0m: [35mInt[0m = [31m"Hello"[0m
            |[31m[31m   |[0m                    ^^^^^^^[0m
            |[31m   |[0m                    Found:    ([32m"Hello"[0m : String)
            |[31m   |[0m                    Required: Int
            |[31m   |[0m
            |[31m   |[0m longer explanation available when compiling with `-explain`
            |2 errors found
            |Build failed
            |java.lang.RuntimeException: Build failed
            |	at io.bazel.rulesscala.scalac.ScalacInvoker.invokeCompiler(ScalacInvoker3.java:43)
            |	at io.bazel.rulesscala.scalac.ScalacWorker.compileScalaSources(ScalacWorker.java:253)
            |	at io.bazel.rulesscala.scalac.ScalacWorker.work(ScalacWorker.java:69)
            |	at io.bazel.rulesscala.worker.Worker.persistentWorkerMain(Worker.java:86)
            |	at io.bazel.rulesscala.worker.Worker.workerMain(Worker.java:39)
            |at io.bazel.rulesscala.scalac.ScalacWorker.main(ScalacWorker.java:33)
      """.trimMargin()

    // when
    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/Hello.scala")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(18, 20),
            """|Type Mismatch Error
                       |Found:    ("Hello" : String)
                       |Required: Int
                       |
                       |longer explanation available when compiling with `-explain`
            """.trimMargin(),
          ),
          errorDiagnostic(
            Position.fromHumanReadable(19, 20),
            """|Type Mismatch Error
                       |Found:    ("Hello" : String)
                       |Required: Int
                       |
                       |longer explanation available when compiling with `-explain`
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse another scala 3 multiple errors`() {
    // given
    val output =
      """|-- [E006] Not Found Error: drd/messaging/src/main/scala/bots/Bot.scala:143:26 
                        |143 |        val messageJson = makeMessage(sendPkt.data)
                        ||                         ^^^^^^^^^^^
                        ||                          Not found: makeMessage
                        ||
                        || longer explanation available when compiling with `-explain`
                        |-- [E006] Not Found Error: drd/messaging/src/main/scala/bots/Bot.scala:153:22 
                        |153 |        client.expect[Message](request)
                        ||                      ^^^^^^^
                        ||                      Not found: type Message
                        ||
                        || longer explanation available when compiling with `-explain`
                        |2 errors found
                        |Build failed
                        |
      """.trimMargin()

    // when
    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/drd/messaging/src/main/scala/bots/Bot.scala")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(143, 26),
            """|Not Found Error
                       |Not found: makeMessage
                       |
                       |longer explanation available when compiling with `-explain`
            """.trimMargin(),
          ),
          errorDiagnostic(
            Position.fromHumanReadable(153, 22),
            """|Not Found Error
                       |Not found: type Message
                       |
                       |longer explanation available when compiling with `-explain`
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse scala 3 diagnostics without ExplanationID`() {
    // given
    val output =
      """|-- Error: path/to/file.scala:27:10 
         |27 |  verride type Env = MyEnv
         |   |          ^^^^
         |   |          end of statement expected but 'type' found
         |-- Warning: path/to/file.scala:13:24 
         |13 |import scala.concurrent.ExecutionContext
         |   |                        ^^^^^^^^^^^^^^^^
         |   |                        unused import
      """.trimMargin()

    // when
    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/path/to/file.scala")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(27, 10),
            "Error\nend of statement expected but 'type' found",
          ),
          warningDiagnostic(
            Position.fromHumanReadable(13, 24),
            "Warning\nunused import",
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse scala warnings`() {
    // given
    val output =
      """          |intellij/release-tool/src/main/scala/com/intellij/releasetool/PluginResolver.scala:14: warning: match may not be exhaustive.
                   |It would fail on the following inputs: Bundled(_), BundledCrossVersion(_, _, _), Direct(_), Empty(), FromSources(_, _), Versioned((x: String forSome x not in "com.intellijUpdaterPlugin"), _, _)
                   |    key match {
                   |    ^
                   |intellij/release-tool/src/main/scala/com/intellij/releasetool/Json.scala:29: warning: trait ScalaObjectMapper in package scala is deprecated (since 2.12.1): ScalaObjectMapper is deprecated because Manifests are not supported in Scala3, you might want to use ClassTagExtensions as a replacement
                   |    val m = new ObjectMapper() with ScalaObjectMapper
                   |                                    ^
                   |four warnings found
                   |INFO: Elapsed time: 35,391s, Critical Path: 17,41s
                   |INFO: 2 processes: 1 disk cache hit, 1 internal.
                   |INFO: Build Event Protocol files produced successfully.
                   |INFO: Build completed successfully, 2 total actions
      """.trimMargin()

    // when
    val label = Label.parse("//intellij/release-tool/src/main/scala/com/intellij/releasetool:releasetool")
    val diagnostics =
      extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(
            Path("/user/workspace/intellij/release-tool/src/main/scala/com/intellij/releasetool/PluginResolver.scala"),
          ),
          Label.parse(label.toString()),
          warningDiagnostic(
            Position.fromHumanReadable(14, 5),
            """match may not be exhaustive.
                   |It would fail on the following inputs: Bundled(_), BundledCrossVersion(_, _, _), Direct(_), Empty(), FromSources(_, _), Versioned((x: String forSome x not in "com.intellijUpdaterPlugin"), _, _)
                   |    key match {
                   |    ^
            """.trimMargin(),
          ),
        ),
        publishDiagnosticsParams(
          TextDocumentIdentifier(
            Path("/user/workspace/intellij/release-tool/src/main/scala/com/intellij/releasetool/Json.scala"),
          ),
          Label.parse(label.toString()),
          warningDiagnostic(
            Position.fromHumanReadable(29, 37),
            """trait ScalaObjectMapper in package scala is deprecated (since 2.12.1): ScalaObjectMapper is deprecated because Manifests are not supported in Scala3, you might want to use ClassTagExtensions as a replacement
                   |    val m = new ObjectMapper() with ScalaObjectMapper
                   |                                    ^
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `parse java messy errors`() {
    // given
    val output =
      """          |server/src/main/java/org/jetbrains/bazel/server/sync/ProjectResolver.java:20: error: symbol not found org.jetbrains.bazel.server.bsp.config.ProjectViewProvider
                   |import org.jetbrains.bazel.server.bsp.config.ProjectViewProvider;
                   |       ^
                   |server/src/main/java/org/jetbrains/bazel/server/sync/ProjectResolver.java:37: error: could not resolve ProjectViewProvider
                   |      ProjectViewProvider projectViewProvider,
                   |      ^
      """.trimMargin()

    // when
    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(
            Path("/user/workspace/server/src/main/java/org/jetbrains/bazel/server/sync/ProjectResolver.java"),
          ),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(20, 8),
            """symbol not found org.jetbrains.bazel.server.bsp.config.ProjectViewProvider
                   |import org.jetbrains.bazel.server.bsp.config.ProjectViewProvider;
                   |       ^
            """.trimMargin(),
          ),
          errorDiagnostic(
            Position.fromHumanReadable(37, 7),
            """could not resolve ProjectViewProvider
                   |      ProjectViewProvider projectViewProvider,
                   |      ^
            """.trimMargin(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should parse java symbol not found error`() {
    // given
    val output =
      """
      server/src/main/java/org/jetbrains/bazel/server/bep/BepServer.java:55: error: cannot find symbol
          return new BepStreamObserver(thi, responseObserver);
                                       ^
        symbol:   variable thi
        location: class BepServer
      """.trimIndent()

    // when
    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/user/workspace/server/src/main/java/org/jetbrains/bazel/server/bep/BepServer.java")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(55, 34),
            """
            cannot find symbol
                return new BepStreamObserver(thi, responseObserver);
                                             ^
              symbol:   variable thi
              location: class BepServer
            """.trimIndent(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should parse multi-line errors`() {
    val output =
      """
      SEVERE: Compilation failure: compile phase failed:
      server/server/src/main/kotlin/org/jetbrains/bazel/server/diagnostics/CompilerDiagnosticParser.kt:10:1: error: example error.
      server/server/src/main/kotlin/org/jetbrains/bazel/server/diagnostics/CompilerDiagnosticParser.kt:57:12: error: none of the following candidates is applicable:
      fun <T> Pair<T, T>.toList(): List<T>
      fun <T> Triple<T, T, T>.toList(): List<T>
      fun <T> Array<out T>.toList(): List<T>
      fun ByteArray.toList(): List<Byte>
      fun ShortArray.toList(): List<Short>
      fun IntArray.toList(): List<Int>
      fun LongArray.toList(): List<Long>
      fun FloatArray.toList(): List<Float>
      fun DoubleArray.toList(): List<Double>
      fun BooleanArray.toList(): List<Boolean>
      fun CharArray.toList(): List<Char>
      fun <T> Enumeration<T>.toList(): List<T>
      fun <T> Iterable<T>.toList(): List<T>
      fun <K, V> Map<out K, V>.toList(): List<Pair<K, V>>
      fun <T> Sequence<T>.toList(): List<T>
      fun CharSequence.toList(): List<Char>
                .toList()
                 ^^^^^^
      INFO: Build succeeded for only 179 of 366 top-level targets
      """.trimIndent()

    val label = Label.parse("//project/src/main/scala/com/example/project:project")
    val diagnostics = extractDiagnostics(output, label)

    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(
            Path("/user/workspace/server/server/src/main/kotlin/org/jetbrains/bazel/server/diagnostics/CompilerDiagnosticParser.kt"),
          ),
          label,
          errorDiagnostic(
            Position.fromHumanReadable(10, 1),
            "example error.",
          ),
          errorDiagnostic(
            Position.fromHumanReadable(57, 12),
            """
            none of the following candidates is applicable:
            fun <T> Pair<T, T>.toList(): List<T>
            fun <T> Triple<T, T, T>.toList(): List<T>
            fun <T> Array<out T>.toList(): List<T>
            fun ByteArray.toList(): List<Byte>
            fun ShortArray.toList(): List<Short>
            fun IntArray.toList(): List<Int>
            fun LongArray.toList(): List<Long>
            fun FloatArray.toList(): List<Float>
            fun DoubleArray.toList(): List<Double>
            fun BooleanArray.toList(): List<Boolean>
            fun CharArray.toList(): List<Char>
            fun <T> Enumeration<T>.toList(): List<T>
            fun <T> Iterable<T>.toList(): List<T>
            fun <K, V> Map<out K, V>.toList(): List<Pair<K, V>>
            fun <T> Sequence<T>.toList(): List<T>
            fun CharSequence.toList(): List<Char>
                      .toList()
                       ^^^^^^
            """.trimIndent(),
          ),
        ),
      )

    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should parse intellij kotlin errors`() {
    // given
    val output =
      """
      Kotlinc Runner: Error: Unresolved reference 'foo'.
        /Users/user/path/to/project/src/Foo.kt:25:90
      Kotlinc Runner: Error: Unresolved reference 'bar'.
        /Users/user/path/to/project/src/Foo.kt:44:33
      Kotlinc Runner: Error: None of the following candidates is applicable:
      fun <T> Array<out T>.toList(): List<T>
      fun ByteArray.toList(): List<Byte>
      fun ShortArray.toList(): List<Short>
      fun <K, V> Map<out K, V>.toList(): List<Pair<K, V>>
      	/Users/user/path/to/project/src/package/Zoo.kt:31:3
      """.trimIndent()

    // when
    val label = Label.parse("//plugin-bazel/src/test/java/org/jetbrains/bazel/server/diagnostics:diagnostics")

    val diagnostics =
      extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/Users/user/path/to/project/src/Foo.kt")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(25, 90),
            "Unresolved reference 'foo'.",
          ),
          errorDiagnostic(
            Position.fromHumanReadable(44, 33),
            "Unresolved reference 'bar'.",
          ),
        ),
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/Users/user/path/to/project/src/package/Zoo.kt")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(31, 3),
            """
              None of the following candidates is applicable:
              fun <T> Array<out T>.toList(): List<T>
              fun ByteArray.toList(): List<Byte>
              fun ShortArray.toList(): List<Short>
              fun <K, V> Map<out K, V>.toList(): List<Pair<K, V>>
            """.trimIndent(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should parse intellij java errors`() {
    // given
    val output =
      """
      Javac Runner: Error: cannot find symbol
        symbol:   variable bar
        location: class Bar
        /Users/user/path/to/project/src/Foo.java:106:26
        code: "bar"
      Javac Runner: Error: reference to foo is ambiguous
        both method foo(java.lang.Integer) in Foo and method foo(java.lang.Double) in Foo match
        /Users/user/path/to/project/src/Foo.java:110:37
        code: "Foo.foo"
      Javac Runner: Error: cannot find symbol
        symbol:   variable zoo
        location: class Zoo
        /Users/user/path/to/project/src/package/Zoo.java:33:44
        code: "zoo"
      """.trimIndent()

    // when
    val label = Label.parse("//plugin-bazel/src/test/java/org/jetbrains/bazel/server/diagnostics:diagnostics")

    val diagnostics =
      extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/Users/user/path/to/project/src/Foo.java")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(106, 26),
            """
              cannot find symbol
                symbol:   variable bar
                location: class Bar
            """.trimIndent(),
          ),
          errorDiagnostic(
            Position.fromHumanReadable(110, 37),
            """
              reference to foo is ambiguous
                both method foo(java.lang.Integer) in Foo and method foo(java.lang.Double) in Foo match
            """.trimIndent(),
          ),
        ),
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/Users/user/path/to/project/src/package/Zoo.java")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(33, 44),
            """
              cannot find symbol
                symbol:   variable zoo
                location: class Zoo
            """.trimIndent(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `should parse intellij errors with special characters in path`() {
    // given
    val specialCharsPathSegment = "path with #!@$%^&(){}[]+=-_"
    val output =
      """
      Javac Runner: Error: cannot find symbol
        symbol:   variable bar
        location: class Bar
        /Users/user/$specialCharsPathSegment/to/project/src/Foo.java:106:26
        code: "bar"
      Javac Runner: Error: cannot find symbol
        symbol:   variable zoo
        location: class Zoo
        /Users/user/$specialCharsPathSegment/to/project/src/package/Zoo.java:33:44
        code: "zoo"  
      Kotlinc Runner: Error: Unresolved reference 'foo'.
        /Users/user/$specialCharsPathSegment/to/project/src/Foo.kt:25:90
      """.trimIndent()

    // when
    val label = Label.parse("//plugin-bazel/src/test/java/org/jetbrains/bazel/server/diagnostics:diagnostics")

    val diagnostics =
      extractDiagnostics(output, label)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/Users/user/$specialCharsPathSegment/to/project/src/Foo.java")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(106, 26),
            """
              cannot find symbol
                symbol:   variable bar
                location: class Bar
            """.trimIndent(),
          ),
        ),
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/Users/user/$specialCharsPathSegment/to/project/src/package/Zoo.java")),
          Label.parse(label.toString()),
          errorDiagnostic(
            Position.fromHumanReadable(33, 44),
            """
              cannot find symbol
                symbol:   variable zoo
                location: class Zoo
            """.trimIndent(),
          ),
        ),
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("/Users/user/$specialCharsPathSegment/to/project/src/Foo.kt")),
          Label.parse(label.toString()),
          errorDiagnostic(
            position = Position.fromHumanReadable(25, 90),
            message = "Unresolved reference 'foo'.",
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }


  @Test
  @EnabledOnOs(OS.WINDOWS)
  fun `BUILD file error with Windows drive letter in path`() {
    // given
    val output =
      """
        [0;31;91mERROR: [0mZ:/buildagent/work/89e70edcf4afcfd6/out/ide-tests/cache/projects/unpacked/simplebazelprojectsfortesting/importrunconfigurations/BUILD:6:13: in something_something rule ... visibility ...
      """.trimIndent()

    // when
    val label = Label.parse("//:calculator")

    val diagnostics =
      extractDiagnostics(output, label, isCommandLineFormattedOutput = true)

    // then
    val expected =
      listOf(
        publishDiagnosticsParams(
          TextDocumentIdentifier(Path("Z:/buildagent/work/89e70edcf4afcfd6/out/ide-tests/cache/projects/unpacked/simplebazelprojectsfortesting/importrunconfigurations/BUILD")),
          label,
          errorDiagnostic(
            Position.fromHumanReadable(6, 13),
            """
               in something_something rule ... visibility ...
            """.trimIndent(),
          ),
        ),
      )
    diagnostics shouldContainExactlyInAnyOrder expected
  }

  private fun publishDiagnosticsParams(
    textDocument: TextDocumentIdentifier?,
    buildTarget: Label,
    vararg diagnostics: Triple<Position, String, DiagnosticSeverity>,
  ): PublishDiagnosticsParams =
    PublishDiagnosticsParams(
      TaskGroupId.EMPTY.task("task-id"),
      textDocument,
      buildTarget,
      diagnostics = diagnostics.map {
        Diagnostic(
          position = it.first,
          message = it.second,
          fileLocation = textDocument?.path,
          targetLabel = buildTarget,
          level = it.third,
        )
      },
      reset = true,
    )

  private fun errorDiagnostic(position: Position, message: String): Triple<Position, String, DiagnosticSeverity> =
    Triple(position, message, DiagnosticSeverity.ERROR)

  private fun warningDiagnostic(position: Position, message: String): Triple<Position, String, DiagnosticSeverity> =
    Triple(position, message, DiagnosticSeverity.WARNING)

  private fun extractDiagnostics(
    output: String,
    buildTarget: Label,
    isCommandLineFormattedOutput: Boolean = false,
  ): List<PublishDiagnosticsParams> =
    DiagnosticsService(workspacePath)
      .extractDiagnostics(
        output.lines(),
        buildTarget,
        TaskGroupId.EMPTY.task("task-id"),
        isCommandLineFormattedOutput,
      )
}
