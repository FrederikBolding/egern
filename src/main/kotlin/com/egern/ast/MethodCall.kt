package com.egern.ast

import com.egern.util.forEach
import com.egern.visitor.Visitor

open class MethodCall(
    val objectId: String,
    open val methodId: String,
    open val args: List<Expr>,
    lineNumber: Int,
    charPosition: Int
) : Expr(lineNumber, charPosition) {
    override fun accept(visitor: Visitor) {
        visitor.preVisit(this)
        args.forEach({ it.accept(visitor) }, doBetween = { visitor.midVisit(this) })
        visitor.postVisit(this)
    }
}