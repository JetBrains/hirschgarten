load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "BazelKotlinFacetEntityUpdaterTest",
    srcs = ["BazelKotlinFacetEntityUpdaterTest.kt"],
    test_package_root = "org.jetbrains",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/kotlin/sync",
        "//rules_intellij/intellij_platform_sdk:kotlin_for_tests",
    ],
)
