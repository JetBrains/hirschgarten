load("@rules_java//java:defs.bzl", "java_binary", "java_library")

java_binary(
    name = "bin_outer",
    srcs = ["BinOuter.java"],
    main_class = "outer.BinOuter",
    visibility = ["//visibility:public"],
    deps = [
        ":lib_outer",
        "@inner//:lib_inner",
    ],
)

java_library(
    name = "lib_outer",
    srcs = ["LibOuter.java"],
    visibility = ["//visibility:public"],
)
