load("@rules_python//python:defs.bzl", "py_library")
load("//:gen.bzl", "gen")

gen(
    name = "gen_part",
    file = "part.py",
    text = "P = 1\n",
)

py_library(
    name = "part",
    srcs = [":gen_part"],
    imports = ["."],
)

gen(
    name = "gen_data",
    file = "data.py",
    text = "from part import P\n\nD = P + 1\n",
)

py_library(
    name = "data",
    srcs = [":gen_data"],
    imports = ["."],
    deps = [":part"],
)

py_library(
    name = "consumer",
    srcs = ["consumer.py"],
    imports = ["."],
    deps = [":data"],
)
