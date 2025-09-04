load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "languages",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/phased",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/api",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/go",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/java",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/kotlin",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/python",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/scala",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/thrift",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/service",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "//third_party/bazel/src/main/protobuf:deps_java_proto",
    ],
)
