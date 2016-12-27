//#excludeif true
//#-
package main

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"image/png"
	"log"
)

func main() {
	//#+
	//#local image="\""+binfile("./image.png","base64")+"\""
	var imageArray, err = base64.StdEncoding.DecodeString( /*$image+")"$*/ /*-*/ "")
	if err != nil {
		log.Fatal(err)
	}
	var imageConfig, errDecode = png.DecodeConfig(bytes.NewBuffer(imageArray))
	if errDecode != nil {
		log.Fatal(err)
	}

	fmt.Printf("Embedded image has size %dx%d\n", imageConfig.Width, imageConfig.Height)

	//#local text=str2java(evalfile("./text.txt"),false)
	fmt.Println( /*$"\""+text+"\""$*/ )
	//#-
}

//#+
