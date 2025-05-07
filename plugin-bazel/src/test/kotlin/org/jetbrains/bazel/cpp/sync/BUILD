load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "BazelCompilerSettingTest",
    srcs = ["BazelCompilerSettingTest.kt"],
    associates = ["//plugin-bazel/src:plugin-bazel-lib"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel",
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/cpp/sync",
        "//rules_intellij/third_party/cpp:cpp_for_tests",
    ],
)

intellij_integration_test_suite(
    name = "BazelConfigurationResolverTest",
    srcs = ["BazelConfigurationResolverTest.kt"],
    associates = ["//plugin-bazel/src:plugin-bazel-lib"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel",
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/cpp/sync",
        "//rules_intellij/third_party/cpp:cpp_for_tests",
    ],
)
