package main

import (
	"example/bindata"
	"fmt"
	"os"
)

//go:generate go-bindata -prefix "../data/" -pkg bindata -o ../bin/src/example/bindata/binasset.go ../data/...
func main() {
	data, err := bindata.Asset("la_espero.txt")
	if err != nil {
		fmt.Printf("Can't find needed asset")
		os.Exit(1)
	}
	fmt.Printf("\n    La Espero\n==================\n%v", string(data))
}
