package main

import (
	"testing"
)

func TestRegister(t *testing.T) {
	if 2+2 == 5 {
		t.Errorf("mathematics are broken")
	}
}
