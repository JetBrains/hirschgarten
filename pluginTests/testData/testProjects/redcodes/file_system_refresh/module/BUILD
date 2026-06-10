load("@rules_java//java:java_import.bzl", "java_import")
load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "my-module",
    srcs = glob(
        [
            "src/**/*",
            "gen/**/*",
        ],
        allow_empty = True,
    ),
    resources = glob(["resources/**/*"]),
    visibility = ["//visibility:public"],
)
