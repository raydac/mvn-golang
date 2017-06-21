package main

import "github.com/raydac/mvn-golang-cvs-test"
import "fmt"

func main() {
	fmt.Printf(mvngolangcvstest.GetSomeText())
	if mvngolangcvstest.GetSomeText() != "some_text_999" {
		panic("Wrong value, expected 'some_text_999'")
	}
}
