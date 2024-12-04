load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

java_library(
    name = "java-lib",
    srcs = ["Lib.java"],
)

java_binary(
    name = "java-binary",
    srcs = ["Main.java"],
    main_class = "Main",
    deps = [":java-lib"],
)

kt_jvm_library(
    name = "kt-lib",
    srcs = ["Lib.kt"],
)

kt_jvm_binary(
    name = "kt-binary",
    srcs = ["Main.kt"],
    main_class = "Main",
    deps = [":kt-lib"],
)
