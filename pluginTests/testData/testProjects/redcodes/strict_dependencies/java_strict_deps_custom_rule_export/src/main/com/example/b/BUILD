load("//:custom_rule.bzl", "custom_java_library")

custom_java_library(
    name = "B",
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//src/main/com/example/c:C",
    ],
)
