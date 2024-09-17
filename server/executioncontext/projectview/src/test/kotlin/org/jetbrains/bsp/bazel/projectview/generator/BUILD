load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "DefaultProjectViewGeneratorTest",
    size = "small",
    src = "DefaultProjectViewGeneratorTest.kt",
    deps = [
        "//server/executioncontext/projectview:parser",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/generator",
    ],
)
