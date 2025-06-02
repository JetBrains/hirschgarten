load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "artifact",
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
        "//commons/src/main/kotlin/com/google/idea/blaze/common",
        "//commons/src/main/kotlin/com/google/idea/blaze/exception",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "@com_google_guava_guava//jar",
        "@error_prone_annotations//jar",
        "@jsr305_annotations//jar",
    ],
)
