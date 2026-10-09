package org.jetbrains.bazel.project

import com.intellij.openapi.util.io.NioFiles
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.TestFixtureImpl
import com.intellij.testFramework.junit5.fixture.testFixture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelOutputBaseFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@BazelTestApplication
@Timeout(30)
internal class BazelOutputBaseFixtureTest {
  private val context by testFixture { initialized(it) {} }

  @Test
  fun `cleanup deletes read-only directories`(): Unit = timeoutRunBlocking {
    val fixtureJob = SupervisorJob(coroutineContext.job)
    val fixture = bazelOutputBaseFixture() as TestFixtureImpl<Path>
    val outputBase = fixture.init(CoroutineScope(coroutineContext + fixtureJob), context).await().first
    val readOnlyDirectory = outputBase.resolve("read-only").createDirectories()
    readOnlyDirectory.resolve("file").writeText("")
    NioFiles.setReadOnly(readOnlyDirectory, true)

    fixtureJob.cancelAndJoin()

    assertThat(outputBase).doesNotExist()
  }

  @Test
  fun `output base name does not depend on the test name`(): Unit = timeoutRunBlocking {
    val fixtureJob = SupervisorJob(coroutineContext.job)
    val fixture = bazelOutputBaseFixture() as TestFixtureImpl<Path>
    val outputBase = fixture.init(CoroutineScope(coroutineContext + fixtureJob), context).await().first

    try {
      assertThat(outputBase.fileName.toString()).matches("ob-[0-9a-f]{8}")
    }
    finally {
      fixtureJob.cancelAndJoin()
    }
  }
}
