load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_test")

java_library(
    name = "java_library",
    srcs = ["TestJavaFile.java"],
    tags = ["manual"],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "java_binary",
    srcs = ["TestJavaBinary.java"],
    main_class = "manual_target.TestJavaBinary",
    tags = ["manual"],
    visibility = ["//visibility:public"],
)

java_test(
    name = "java_test",
    srcs = ["JavaTest.java"],
    tags = ["manual"],
    test_class = "manual_target.JavaTest",
    visibility = ["//visibility:public"],
)
