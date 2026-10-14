load("@rules_java//java:java_library.bzl", "java_library")

####

java_library(
    name = "main1",
    srcs = ["Main.java"],
    deps = ["//:lib_a1"],
)

java_library(
    name = "lib_a1",
    srcs = ["lib1/A.java"],
    deps = ["//:lib_b1"],
)

java_library(
    name = "lib_b1",
    srcs = ["lib1/B.java"],
)

#####

java_library(
    name = "main2",
    srcs = ["Main.java"],
    deps = ["//:lib_a2"],
)

java_library(
    name = "lib_a2",
    srcs = ["lib2/A.java"],
    deps = ["//:lib_b2"],
)

java_library(
    name = "lib_b2",
    srcs = ["lib2/B.java"],
)
