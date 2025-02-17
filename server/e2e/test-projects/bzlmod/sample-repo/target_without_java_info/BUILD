# These targets should NOT be imported as modules into IDEA because we don't have enough information,
# and to avoid red code because of shared sources
filegroup(
    name = "filegroup",
    srcs = [
        "Example.java",
    ],
)

genrule(
    name = "genrule",
    srcs = ["Example.kt"],
    outs = ["foo"],
    cmd = "echo 'echo foo' > $@",
    executable = True,
)
