load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "common",
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
        "//third_party/java/auto_value",
        "@com_google_guava_guava//jar",
        "@error_prone_annotations//jar",
        "@jsr305_annotations//jar",
    ],
)
