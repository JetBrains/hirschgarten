load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")

exports_files(["main/resources/META-INF/base.xml"])

proto_library(
    name = "starlark_annotations_proto",
    srcs = glob(["proto/**/*.proto"]),
)

java_proto_library(
    name = "starlark_annotations",
    deps = [":starlark_annotations_proto"],
)

kt_jvm_library(
    name = "intellij-bsp",
    neverlink = True,
    exports = [
        "//plugin-bsp/src:intellij-bsp",
    ],
)

kt_jvm_library(
    name = "intellij-bazel",
    srcs = glob([
        "main/kotlin/**/*.java",
        "main/kotlin/**/*.kt",
        "main/gen/**/*.java",
    ]),
    resources = glob([
        "main/resources/**",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        ":intellij-bsp",
        ":starlark_annotations",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel:bsp_export",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
        "@rules_intellij//third_party/performance",
        "@rules_intellij//third_party/python",
    ],
)

intellij_integration_test_suite(
    name = "plugin_tests",
    srcs = glob([
        "test/kotlin/org/jetbrains/**/*.kt",
    ]),
    associates = [":intellij-bazel"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        ":intellij-bazel",
        "//plugin-bsp/src:intellij-bsp",
    ],
    deps = [
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
