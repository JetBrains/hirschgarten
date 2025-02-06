load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

proto_library(
    name = "intellij_plugin_deploy_target_info_lib",
    srcs = ["intellij_plugin_deploy_target_info.proto"],
)

java_proto_library(
    name = "intellij_plugin_deploy_target_info",
    deps = [":intellij_plugin_deploy_target_info_lib"],
)

kt_jvm_library(
    name = "intellij",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/impl",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/util",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/notifications",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        ":intellij_plugin_deploy_target_info",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/devkit",
    ],
)
