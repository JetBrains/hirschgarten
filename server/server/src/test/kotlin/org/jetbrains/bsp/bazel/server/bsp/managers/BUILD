load("@//server/rules/kotlin:junit5.bzl", "kt_test")

kt_test(
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

kt_test(
    name = "BzlModGraphTest",
    size = "small",
    src = "BzlModGraphTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/managers",
    ],
)
