load("@rules_java//java:defs.bzl", "java_binary", "java_library")

java_binary(
    name = "bin_inner",
    srcs = ["BinInner.java"],
    main_class = "inner.BinInner",
    visibility = ["//visibility:public"],
    deps = [
        "//:lib_inner",
    ],
)

java_library(
    name = "lib_inner",
    srcs = ["LibInner.java"],
    visibility = ["//visibility:public"],
)
