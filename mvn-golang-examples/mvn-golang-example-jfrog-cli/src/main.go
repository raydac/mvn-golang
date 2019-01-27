package main

import "fmt"

var Buildstamp string
var svnRevision string

func main() {
	fmt.Printf(Buildstamp + " " + svnRevision + "\n")
}
