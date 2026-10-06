load("@rules_java//java:java_library.bzl", "java_library")
load("//defs:defs.bzl", "java_and_py_library")

java_library(
    name = "jvm_part",
    srcs = ["Shared.java"],
)

java_and_py_library(
    name = "shared",
    srcs = ["shared.py"],
    jvm = ":jvm_part",
)
