sh_binary(
    name = "bin",
    srcs = ["bin.sh"],
)

sh_library(
    name = "lib",
    srcs = ["lib.sh"],
    deps = ["//first_level_dep:lib"],
)

sh_test(
    name = "test",
    srcs = ["test.sh"],
)
