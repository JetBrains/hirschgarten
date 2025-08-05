java_library(
    name = "myLib",
    my_srcs = [<error descr="File does not exist">"Unresolved.java"</error>],
)
