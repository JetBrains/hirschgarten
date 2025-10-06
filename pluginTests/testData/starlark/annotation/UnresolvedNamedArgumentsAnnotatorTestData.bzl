def foo(a): a

foo(4, <error descr="Cannot find a parameter with this name: b">b</error> = 2)

def bar(**kwargs): 42

bar(a = 4, b = 2)