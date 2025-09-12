load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "DependencyGraphTest",
    size = "small",
    src = "DependencyGraphTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
    ],
)
