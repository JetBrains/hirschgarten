load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "utils",
    srcs = [
        "DelimitedMessageReader.kt",
        "InternalAspectsResolver.kt",
    ],
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//server/bazelrunner",
        "//server/commons",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "@maven//:io_grpc_grpc_protobuf",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)

kt_jvm_library(
    name = "fileutils",
    srcs = ["FileUtils.kt"],
    visibility = ["//server/server:__subpackages__"],
)
