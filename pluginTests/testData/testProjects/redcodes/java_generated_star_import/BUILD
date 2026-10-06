load("@rules_java//java:java_library.bzl", "java_library")

genrule(
    name = "gen_file",
    outs = ["FileGen.java"],
    cmd = "printf 'package com.example.genfile;\\npublic class FileGen {}\\n' > $@",
)

java_library(
    name = "lib",
    srcs = [
        "Main.java",
        ":gen_file",
    ],
)
