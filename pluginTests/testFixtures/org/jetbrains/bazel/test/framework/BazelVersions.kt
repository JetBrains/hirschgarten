package org.jetbrains.bazel.test.framework

import org.jetbrains.bazel.commons.BazelRelease

/**
 * The Bazel versions that a test runs against.
 *
 * A test class that covers more than one version keeps one nested class per version, because the project
 * fixture takes the version as a constructor argument:
 * ```
 * @TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * abstract class MyTest(override val bazelVersion: String) : BazelVersionedTest {
 *   @BazelTestApplication
 *   class Bazel8 : MyTest(BazelVersions.BAZEL_8)
 *
 *   @BazelTestApplication
 *   class Bazel9 : MyTest(BazelVersions.BAZEL_9)
 *
 *   private val project by bazelProjectFixture("my/project", bazelVersion = bazelVersion)
 *
 *   @Test
 *   fun testSomething(): Unit = timeoutRunBlocking { ... }
 * }
 * ```
 *
 * Implementing [BazelVersionedTest] is only needed for a test class that has a single test restricted to some
 * of its versions with [org.jetbrains.bazel.test.framework.annotation.EnabledOnBazel].
 */
object BazelVersions {
  const val BAZEL_7: String = "7.7.1"

  const val BAZEL_8: String = "8.8.0"

  const val BAZEL_9: String = "9.2.0"
}

/** A test class that runs against a Bazel version chosen by its nested class. */
interface BazelVersionedTest {
  val bazelVersion: String
}

val BazelVersionedTest.majorBazelVersion: Int get() = requireNotNull(BazelRelease.fromReleaseString(bazelVersion)).major
