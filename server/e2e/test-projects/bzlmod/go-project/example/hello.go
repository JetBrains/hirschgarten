package main

import (
	"fmt"
	"go-project-e2e/lib"
)

func main() {
	_ = cases.Compact
	fmt.Println(lib.SayHello())
}
