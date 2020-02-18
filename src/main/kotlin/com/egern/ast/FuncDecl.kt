package com.egern.ast

import com.egern.symbols.SymbolTable
import com.egern.visitor.Visitor

class FuncDecl(val id: String, val params: List<String>, private val funcBody: FuncBody) : ASTNode(), Scopable {
    override lateinit var symbolTable: SymbolTable
    lateinit var startLabel: String
    lateinit var endLabel: String

    override fun accept(visitor: Visitor) {
        visitor.preVisit(this)
        funcBody.accept(visitor)
        visitor.postVisit(this)
    }
}