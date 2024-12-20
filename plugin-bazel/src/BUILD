load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

exports_files(["main/resources/META-INF/base.xml"])

proto_library(
    name = "starlark_annotations_proto",
    srcs = glob(["main/proto/**/*.proto"]),
)

java_proto_library(
    name = "starlark_annotations",
    deps = [":starlark_annotations_proto"],
)

kt_jvm_library(
    name = "intellij-bsp",
    neverlink = True,
    visibility = ["//plugin-bazel:__subpackages__"],
    exports = [
        "//plugin-bsp/src:intellij-bsp",
    ],
)

kt_jvm_library(
    name = "intellij-bazel",
    visibility = ["//visibility:public"],
    exports = [
        "//plugin-bazel/src:intellij-bazel-unpackaged",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/debug",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/extension",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/hotswap",
    ],
)

kt_jvm_library(
    name = "intellij-bazel-unpackaged",
    srcs = glob([
        "main/kotlin/**/*.kt",
        "main/gen/**/*.java",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        ":intellij-bsp",
        ":starlark_annotations",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/base",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/highlighting:higlighting",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/lexer",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/run",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel:bsp_export",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
        "@rules_intellij//third_party/performance",
        "@rules_intellij//third_party/python",
    ],
)

kt_jvm_library(
    name = "intellij-bazel-resources",
    resources = glob([
        "main/resources/**",
    ]),
    visibility = ["//visibility:public"],
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
        "//plugin-bsp/src:test_fixtures",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
