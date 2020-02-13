package com.egern.ast

import com.egern.visitor.Visitor

class ArithExpr(val lhs: Expr, val rhs: Expr, val op: String) : Expr() {
    override fun accept(visitor: Visitor) {
        visitor.preVisit(this)
        lhs.accept(visitor)
        rhs.accept(visitor)
    }
}