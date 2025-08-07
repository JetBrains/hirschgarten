java_library(
    name = "myLib",
    my_srcs = <error descr="Glob resolves to an empty set of files">glob</error>([<error descr="Glob pattern does not match any files">"**/*.nonexistent"</error>]),
)