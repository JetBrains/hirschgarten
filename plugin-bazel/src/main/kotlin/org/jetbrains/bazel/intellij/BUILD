load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

proto_library(
    name = "intellij_plugin_deploy_target_info_lib",
    srcs = ["intellij_plugin_deploy_target_info.proto"],
)

java_proto_library(
    name = "intellij_plugin_deploy_target_info",
    visibility = ["//visibility:public"],
    deps = [":intellij_plugin_deploy_target_info_lib"],
)

kt_jvm_library(
    name = "intellij",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/intellij:intellij_plugin_deploy_target_info",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/tasks",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/notifications",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/devkit",
        "//sdkcompat",
    ],
)
