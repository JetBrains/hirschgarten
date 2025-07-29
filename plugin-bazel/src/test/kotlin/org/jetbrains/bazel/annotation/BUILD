load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "PublicApiCheckTest",
    srcs = ["PublicApiCheckTest.kt"],
    resources = [
        "public-api-classes.txt",
        "public-api-fields.txt",
        "public-api-methods.txt",
        "//plugin-bazel",
    ],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        "//plugin-bazel",
    ],
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/resourceUtil",
        "//rules_intellij/intellij_platform_sdk:java_for_tests",
        "//rules_intellij/intellij_platform_sdk:kotlin_for_tests",
        "//rules_intellij/third_party/android:android_for_tests",
        "//rules_intellij/third_party/code_with_me:code_with_me_for_tests",
        "//rules_intellij/third_party/go:go_for_tests",
        "//rules_intellij/third_party/python:python_for_tests",
        "//rules_intellij/third_party/terminal:terminal_for_tests",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)

intellij_integration_test_suite(
    name = "RemoveWithSdkCompatTest",
    srcs = ["RemoveWithSdkCompatTest.kt"],
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
    ],
    data = [":sdkCompatStructure"],
    resources = [
        "//plugin-bazel",
    ],
    test_package_root = "org.jetbrains",
    runtime_deps = ["//plugin-bazel"],
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/resourceUtil",
        "//rules_intellij/intellij_platform_sdk:java_for_tests",
        "//rules_intellij/intellij_platform_sdk:kotlin_for_tests",
        "//rules_intellij/third_party/android:android_for_tests",
        "//rules_intellij/third_party/code_with_me:code_with_me_for_tests",
        "//rules_intellij/third_party/go:go_for_tests",
        "//rules_intellij/third_party/python:python_for_tests",
        "//rules_intellij/third_party/terminal:terminal_for_tests",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)

genquery(
    name = "sdkCompatStructure",
    expression = "deps(//sdkcompat)",
    opts = ["--output=label"],
    scope = ["//sdkcompat"],
)
