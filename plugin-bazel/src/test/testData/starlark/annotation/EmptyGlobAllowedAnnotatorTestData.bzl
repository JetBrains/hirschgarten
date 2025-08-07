java_library(
    name = "myLib",
    my_srcs = glob(["*.java"], ["example*.java"], allow_empty = True),
)