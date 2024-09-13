load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "fixtures",
    testonly = True,
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel/src/test:__subpackages__"],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
        "@rules_intellij//intellij_platform_sdk:plugin_api_for_tests",
        "@rules_intellij//testing:lib",
    ],
)
