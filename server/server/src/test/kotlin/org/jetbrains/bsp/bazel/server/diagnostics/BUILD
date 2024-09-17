load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "DiagnosticsServiceTest",
    size = "small",
    src = "DiagnosticsServiceTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/diagnostics",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
