load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "proto",
    srcs = glob(
        [
            "*.java",
            "*.kt",
        ],
        allow_empty = True,
    ),
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//og/shared/src/main/java/com/google/idea/blaze/common",
        "@com_google_guava_guava//jar",
        "@protobuf//:protobuf_java",
    ],
)
