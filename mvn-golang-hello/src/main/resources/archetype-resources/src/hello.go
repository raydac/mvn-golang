package ${package}

import "fmt"

func Hello(text string) string {
	return text
}

func main() {
	fmt.Printf(Hello("Hello, world.\n"))
}
