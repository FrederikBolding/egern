package com.egern

import MainLexer
import MainParser
import com.egern.antlr.ThrowingErrorListener
import com.egern.ast.BuildASTVisitor
import com.egern.ast.Program
import com.egern.codegen.CodeGenerationVisitor
import com.egern.codegen.PreCodeGenerationVisitor
import com.egern.symbols.SymbolVisitor
import com.egern.types.TypeCheckingVisitor
import com.egern.visitor.PrintProgramVisitor
import com.egern.visitor.PrintSymbolTableVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun main(args: Array<String>) {
    val quiet = "-q" !in args
    val doPrint = "-p" in args

    if (quiet) {
        println("Egern Compiler v0.1.0! Input something + CTRL+D to run")
    }

    val input = CharStreams.fromStream(System.`in`)
    val lexer = MainLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = MainParser(tokens)

    parser.removeErrorListeners()
    parser.addErrorListener(ThrowingErrorListener.INSTANCE)

    val cst = parser.prog()
    val ast = BuildASTVisitor().visit(cst) as Program

    if (doPrint) {
        val printProgramVisitor = PrintProgramVisitor(4)
        ast.accept(printProgramVisitor)
    }

    val symbolVisitor = SymbolVisitor()
    ast.accept(symbolVisitor)

    if (doPrint) {
        val printVisitor = PrintSymbolTableVisitor()
        ast.accept(printVisitor)
    }

    val typeCheckingVisitor = TypeCheckingVisitor(symbolVisitor.currentTable)
    ast.accept(typeCheckingVisitor)

    val preCodeGenerationVisitor = PreCodeGenerationVisitor()
    ast.accept(preCodeGenerationVisitor)

    val codeGenVisitor = CodeGenerationVisitor(symbolVisitor.currentTable)
    ast.accept(codeGenVisitor)
}

fun hello(): String {
    return "hello"
}