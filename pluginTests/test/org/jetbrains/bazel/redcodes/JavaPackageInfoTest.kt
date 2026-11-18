package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.testFramework.common.timeoutRunBlocking
import com.siyeh.ig.javadoc.MissingPackageInfoInspection
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * The sources of `//module:lib` under `src/main/java` merge into one source root at `src/main/java/com/example`, whose
 * `packageMatchesDirectory` is `false`. The sources next to the BUILD file keep single-file source roots.
 * The packages `com.example.server` and `com.example.top` each have a `package-info.java` in their directory.
 * The "Missing 'package-info.java'" inspection must find it, and each package must have its annotation.
 */
@BazelTestApplication
class JavaPackageInfoTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/java_package_info",
    configure = { it.enableInspections(MissingPackageInfoInspection()) },
  )

  @Test
  fun testPackageInfoIsFound(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("module/src/main/java/com/example/server/package-info.java")
      fixture.checkHighlighting("module/src/main/java/com/example/server/Server.java")
      fixture.checkHighlighting("module/package-info.java")
      fixture.checkHighlighting("module/Top.java")
    }

    for (packageName in listOf("com.example.server", "com.example.top")) {
      val annotation = readAction {
        JavaPsiFacade.getInstance(fixture.project).findPackage(packageName)
          ?.annotationList
          ?.findAnnotation("java.lang.Deprecated")
      }
      withClue("@Deprecated of the package $packageName") { annotation.shouldNotBeNull() }
    }
  }
}
