package org.jetbrains.bazel.test.framework.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class BazelTest(
  val kind: TestKind = TestKind.UNIT_TEST,
)

enum class TestKind {
  UNIT_TEST,
  IDE_STARTER
}
