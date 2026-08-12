load(":macro_that_changes_name.bzl", "my_java_library")

my_java_library(
    name = "java_library",
    srcs = ["Hello.java"],
)

exports_files(
    ["Hello.java"],
    ["//visibility:public"],
)
