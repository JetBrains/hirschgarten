load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "bep",
    srcs = glob(["*.kt", "*.java"], allow_empty = True),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "@maven//:com_google_code_findbugs_jsr305",
        "//third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto:build_event_stream_java_proto",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
