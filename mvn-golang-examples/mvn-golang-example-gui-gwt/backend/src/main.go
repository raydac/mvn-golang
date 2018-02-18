package main

import (
	"bytes"
	"encoding/json"
	"front/bindata"
	"github.com/skratchdot/open-golang/open"
	"io"
	"log"
	"mime"
	"net"
	"net/http"
	pathop "path"
	"strconv"
	"time"
)

//go:generate go-bindata -prefix "../../frontend/target/frontend.out" -pkg bindata -o ../bin/src/front/bindata/binasset.go ../../frontend/target/frontend.out/...

type data_struct struct {
	Data string
}

type push_event struct {
	Time int64
}

func makeTimestamp() int64 {
	return time.Now().UnixNano() / (int64(time.Millisecond) / int64(time.Nanosecond))
}

var eventmap = map[string](func(data_struct) data_struct){
	"buttons/send": eventButtonSend,
}

func eventButtonSend(data data_struct) data_struct {
	data.Data = data.Data + "! Hello from the Go side!"
	return data
}

func push_handler(rw http.ResponseWriter, req *http.Request) {
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Cache-Control", "no-cache")
	var data push_event
	data.Time = makeTimestamp()
	js, err := json.Marshal(data)
	if err != nil {
		log.Panic(err)
	}
	rw.Write(js)
}

func static_handler(rw http.ResponseWriter, req *http.Request) {
	if req.Method == http.MethodGet {
		var path string = req.URL.Path
		if path == "" {
			path = "index.html"
		}

		if path == "__mailbox__" {
			push_handler(rw, req)
		} else {
			if bs, err := bindata.Asset(path); err != nil {
				log.Printf("Can't find resource : %s", path)
				rw.WriteHeader(http.StatusNotFound)
			} else {
				mime := mime.TypeByExtension(pathop.Ext(path))
				var reader = bytes.NewBuffer(bs)
				if mime == "" {
					mime = http.DetectContentType(bs)
				}
				log.Printf("Resource '%s' mime=%s", path, mime)
				rw.Header().Set("Content-Type", mime)
				rw.Header().Set("Cache-Control", "no-cache")
				io.Copy(rw, reader)
			}
		}
	} else if req.Method == http.MethodPost {
		path := req.URL.Path
		log.Printf("Incoming event : %s", path)

		eventfunc := eventmap[path]

		if eventfunc == nil {
			log.Panic("Can't find event processor for " + path)
		}

		decoder := json.NewDecoder(req.Body)

		var t data_struct
		err := decoder.Decode(&t)

		if err != nil {
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			log.Print(err)
		} else {
			t = eventfunc(t)
			js, error := json.Marshal(t)
			if error != nil {
				http.Error(rw, error.Error(), http.StatusInternalServerError)
			} else {
				rw.Header().Set("Content-Type", "application/json")
				rw.Header().Set("Cache-Control", "no-cache")
				rw.Write(js)
			}
		}
	}
}

func findFreePort() (int, error) {
	addr, err := net.ResolveTCPAddr("tcp", "127.0.0.1:0")
	if err != nil {
		return 0, err
	}

	l, err := net.ListenTCP("tcp", addr)
	if err != nil {
		return 0, err
	}
	defer l.Close()
	return l.Addr().(*net.TCPAddr).Port, nil
}

func main() {
	port, err := findFreePort()

	if err != nil {
		log.Fatal(err)
	}

	log.Printf("Application server address 127.0.0.1:%d", port)

	http.Handle("/", http.StripPrefix("/", http.HandlerFunc(static_handler)))
	listenStartBrowserAndServe("127.0.0.1:" + strconv.Itoa(port))
}

type tcpKeepAliveListener struct {
	*net.TCPListener
}

func listenStartBrowserAndServe(addr string) error {
	server := &http.Server{Addr: addr, Handler: nil}

	ln, err := net.Listen("tcp", addr)
	if err != nil {
		return err
	}

	err = open.Run("http://" + addr + "/index.html")
	if err != nil {
		return err
	}

	return server.Serve(tcpKeepAliveListener{ln.(*net.TCPListener)})

}
