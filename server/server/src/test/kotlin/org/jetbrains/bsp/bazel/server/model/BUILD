load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "ProjectTest",
    size = "small",
    src = "ProjectTest.kt",
    deps = [
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
    ],
)
