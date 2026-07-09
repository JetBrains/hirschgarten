package main

import "github.com/google/uuid"

func Main() {
	_ = uuid.New()
	uuid.<error>Missing</error>()
}
