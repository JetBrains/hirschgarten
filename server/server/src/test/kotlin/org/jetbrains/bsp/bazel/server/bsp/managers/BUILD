load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BazelBspLanguageExtensionsGeneratorTest",
    size = "small",
    src = "BazelBspLanguageExtensionsGeneratorTest.kt",
    deps = [
        "//server/commons",
        "//server/install:install-lib",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/managers",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
    ],
)

kt_junit5_test(
    name = "BzlModGraphTest",
    size = "small",
    src = "BzlModGraphTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/managers",
    ],
)
