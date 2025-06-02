load("@rules_java//java:defs.bzl", "java_library", "java_test")

java_test(
    name = "LabelTest",
    size = "small",
    srcs = ["LabelTest.java"],
    deps = [
        "//commons/src/main/kotlin/com/google/idea/blaze/common",
        "//third_party/java/junit",
        "//third_party/java/truth",
    ],
)

java_library(
    name = "test_utils",
    srcs = [
        "LoggingContext.java",
        "NoopContext.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/com/google/idea/blaze/common",
        "//third_party/java/flogger",
    ],
)

java_test(
    name = "TargetTreeTest",
    size = "small",
    srcs = ["TargetTreeTest.java"],
    deps = [
        "//commons/src/main/kotlin/com/google/idea/blaze/common",
        "//commons/src/main/kotlin/com/google/idea/blaze/exception",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@maven//:com_google_guava_guava",
    ],
)

java_test(
    name = "AtomicFileWriterTest",
    size = "small",
    srcs = ["AtomicFileWriterTest.java"],
    deps = [
        "//commons/src/main/kotlin/com/google/idea/blaze/common",
        "//commons/src/main/kotlin/com/google/idea/blaze/exception",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@maven//:com_google_guava_guava",
    ],
)
