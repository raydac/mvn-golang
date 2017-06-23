package main

import "github.com/raydac/mvn-golang-cvs-test"
import "fmt"

func main() {
	fmt.Printf(mvngolangcvstest.GetSomeText())
	if mvngolangcvstest.GetSomeText() != "some_text" {
		panic("Wrong value, expected 'some_text'")
	}
}
