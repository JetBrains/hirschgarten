load("@rules_java//java:java_library.bzl", "java_library")

def my_java_library(name, srcs):
    java_library(
        name = "my_" + name,
        srcs = srcs,
        visibility = ["//visibility:public"],
    )
