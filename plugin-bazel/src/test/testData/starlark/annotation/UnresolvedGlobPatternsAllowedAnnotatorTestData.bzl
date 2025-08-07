java_library(
    name = "myLib",
    my_srcs = glob(["**/*.nonexistent"], allow_empty = True),
)