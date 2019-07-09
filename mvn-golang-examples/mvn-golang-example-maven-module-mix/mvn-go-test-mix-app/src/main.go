package main

import (
	"com.igormaznitsa/testmixproxy"
	"com.igormaznitsa/testmixterminal"
	"fmt"
	"os"
)

func main() {
	if len(os.Args) > 1 {
		arg := os.Args[1]
		if arg == "proxy" {
			fmt.Println("Selected proxy")
			testmixproxy.StartProxy()
		} else if arg == "terminal" {
			fmt.Println("Selected terminal")
			testmixterminal.StartTerminal()
		} else {
			fmt.Println("Hello world!")
		}
	} else {
		fmt.Println("Select 'proxy' or 'teminal'")
	}
}
