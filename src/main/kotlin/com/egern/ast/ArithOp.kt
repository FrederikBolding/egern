package com.egern.ast

enum class ArithOp(val value: String) {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIVIDE("/");

    companion object {
        private val map = ArithOp.values().associateBy(ArithOp::value)
        fun fromString(type: String) = map[type]
    }
}