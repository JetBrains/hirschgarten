load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "InternalAspectsResolverTest",
    size = "small",
    src = "InternalAspectsResolverTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/utils",
    ],
)

kt_junit5_test(
    name = "DelimitedMessageReaderTest",
    size = "small",
    src = "DelimitedMessageReaderTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/utils",
        "@bazel_bep//jar",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    ],
)
