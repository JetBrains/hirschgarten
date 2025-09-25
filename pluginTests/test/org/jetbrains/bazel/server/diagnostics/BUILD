load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "DiagnosticsServiceTest",
    size = "small",
    src = "DiagnosticsServiceTest.kt",
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/diagnostics",
    ],
)
