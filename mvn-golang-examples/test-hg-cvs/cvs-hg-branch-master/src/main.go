package main

import "bitbucket.org/raydac/mvn-plugin-cvs-hg-test"
import "fmt"

func main() {
	fmt.Printf(mvngolangcvstest.GetSomeText())
	if mvngolangcvstest.GetSomeText() != "some_text_master_current" {
		panic("Wrong value, expected 'some_text_master_current'")
	}
}
