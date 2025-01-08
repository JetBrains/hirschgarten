load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "DependencyGraphTest",
    size = "small",
    src = "DependencyGraphTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/dependencygraph",
    ],
)
