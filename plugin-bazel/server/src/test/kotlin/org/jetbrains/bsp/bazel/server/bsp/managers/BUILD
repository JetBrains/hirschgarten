load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BazelBspLanguageExtensionsGeneratorTest",
    size = "small",
    src = "BazelBspLanguageExtensionsGeneratorTest.kt",
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/install:install-lib",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/managers",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
    ],
)

kt_junit5_test(
    name = "BzlModGraphTest",
    size = "small",
    src = "BzlModGraphTest.kt",
    deps = [
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/managers",
    ],
)
