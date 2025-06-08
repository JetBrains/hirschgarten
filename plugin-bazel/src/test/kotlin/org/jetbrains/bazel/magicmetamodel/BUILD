load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

# TODO: split when the src target is split
intellij_integration_test_suite(
    name = "magicmetamodel",
    srcs = glob([
        "**/*.kt",
    ]),
    associates = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel/impl/updaters",
    ],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        "//plugin-bazel",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl",
        "//rules_intellij/intellij_platform_sdk:java_for_tests",
        "//rules_intellij/third_party/go:go_for_tests",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
