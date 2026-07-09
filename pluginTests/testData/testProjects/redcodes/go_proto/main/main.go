package main

import "example.com/proto/greeting"

func Main() {
	req := &greeting.HelloRequest{}
	_ = req.GetHello()
	_ = &greeting.<error>Missing</error>{}
}
