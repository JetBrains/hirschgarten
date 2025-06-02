load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "bsp",
    srcs = glob([
        "*.java",
        "*.kt",
    ]),
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/bazelrunner",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
        "//third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto:build_event_stream_java_proto",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
