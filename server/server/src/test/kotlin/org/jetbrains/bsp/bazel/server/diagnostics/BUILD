load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "DiagnosticsServiceTest",
    size = "small",
    src = "DiagnosticsServiceTest.kt",
    deps = [
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/diagnostics",
    ],
)
