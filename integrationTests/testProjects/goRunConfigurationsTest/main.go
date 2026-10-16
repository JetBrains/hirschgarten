package main

import (
	"fmt"
	"os"
)

var (
	precedenceA     = "unset"
	precedenceB     = "unset"
	compilationMode = "other"
)

func main() {
	fmt.Printf("Program arguments: %s\n", os.Args[1:])
	fmt.Printf("Environment variable EXAMPLE_ENV: %s\n", os.Getenv("EXAMPLE_ENV"))
	fmt.Printf("Flag precedence: a=%s b=%s\n", precedenceA, precedenceB)
	fmt.Printf("Compilation mode: %s\n", compilationMode)
	fmt.Printf("Execution finished\n")
}
