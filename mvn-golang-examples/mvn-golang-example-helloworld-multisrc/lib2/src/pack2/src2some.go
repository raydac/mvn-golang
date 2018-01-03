package pack2

import (
    "pack1"
)

func Pack2() string {
    return "Pack2-" + pack1.Pack1()
}