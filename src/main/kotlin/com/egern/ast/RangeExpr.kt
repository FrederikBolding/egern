package com.egern.ast

import com.egern.visitor.Visitor

class RangeExpr(
    val lhs: Expr,
    val rhs: Expr,
    val excluding: Boolean,
    lineNumber: Int,
    charPosition: Int
) : Expr(lineNumber, charPosition) {
    override fun accept(visitor: Visitor) {
        lhs.accept(visitor)
        visitor.midVisit(this)
        rhs.accept(visitor)
        visitor.postVisit(this)
    }
}