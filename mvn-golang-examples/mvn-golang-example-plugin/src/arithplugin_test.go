package main

import (
	"testing"
)

func TestAdd(t *testing.T) {
	if Add(4, 7) != 11 {
		t.Fatalf("Wrong Add result")
	}
}

func TestSub(t *testing.T) {
	if Sub(4, 7) != -3 {
		t.Fatalf("Wrong Sub result")
	}
}
