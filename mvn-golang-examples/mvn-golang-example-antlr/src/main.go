package main

import (
	"fmt"
	"github.com/antlr/antlr4/runtime/Go/antlr"
	"parser"
)

type TreeShapeListener struct {
	*parser.BaseJSONListener
}

func NewTreeShapeListener() *TreeShapeListener {
	return new(TreeShapeListener)
}

func (this *TreeShapeListener) EnterEveryRule(ctx antlr.ParserRuleContext) {
	fmt.Println(ctx.GetText())
}

func main() {
	input := antlr.NewInputStream("{\"title\":\"Person\",\"type\":\"object\"}")
	lexer := parser.NewJSONLexer(input)
	stream := antlr.NewCommonTokenStream(lexer, 0)
	p := parser.NewJSONParser(stream)
	p.AddErrorListener(antlr.NewDiagnosticErrorListener(true))
	p.BuildParseTrees = true
	tree := p.Json()
	antlr.ParseTreeWalkerDefault.Walk(NewTreeShapeListener(), tree)
}
