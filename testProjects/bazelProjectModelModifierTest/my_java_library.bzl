load("@rules_java//java:java_library.bzl", "java_library")

def my_java_library(name, srcs, dependencies):
    java_library(
        name = name,
        srcs = srcs,
        deps = dependencies,
    )

