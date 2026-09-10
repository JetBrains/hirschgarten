load("@rules_java//java:java_library.bzl", "java_library")

def public_java_library(name, srcs, deps = []):
    java_library(
        name = name,
        srcs = srcs,
        deps = deps,
        visibility = ["//visibility:public"],
    )
