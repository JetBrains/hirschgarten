package main

import (
	"fmt"
	"os"
)

func main() {
	fmt.Printf("Program arguments: %s\n", os.Args[1:])
	fmt.Printf("Environment variable EXAMPLE_ENV: %s\n", os.Getenv("EXAMPLE_ENV"))
	fmt.Printf("Execution finished\n")
}
