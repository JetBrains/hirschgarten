load("@rules_cc//cc:defs.bzl", "cc_test")

cc_test(
    name = "gtest",
    srcs = [
        "ExampleTestSuite.cpp",
        "TestInHeader.h",
    ],
    deps = ["@googletest//:gtest_main"],
)

cc_test(
    name = "catch",
    srcs = ["TestBase.cpp"],
    args = [
        "--order",
        "decl",
    ],
    deps = ["@catch2//:catch2_main"],
)
