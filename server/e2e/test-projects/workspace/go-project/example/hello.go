package main

import (
    "example.com/lib"
    "fmt"
	"golang.org/x/text/cases"
)

func main() {
	_ = cases.Compact
	fmt.Println(lib.SayHello())
}
