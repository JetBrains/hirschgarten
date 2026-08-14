load("@rules_scala//scala:scala.bzl", "scala_test")

scala_test(
    name = "test",
    srcs = [":FooTest.scala"],
    deps = ["//src/main/com/example/foo:example-lib"],
)
