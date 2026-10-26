load("@rules_java//java:defs.bzl", "java_library")

# Hidden from the IDE. Nothing depends on it through `deps`; IDE-visible
# targets only reference it through `data` or a custom label attribute.
java_library(
    name = "hidden",
    srcs = ["Hidden.java"],
    tags = [
        "manual",
        "no-ide",
    ],
    visibility = ["//visibility:public"],
)
