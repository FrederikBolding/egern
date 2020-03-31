package com.egern.visitor

import com.egern.ast.*
import com.egern.types.*

class PrintProgramVisitor(private val indentation: Int = 4) : Visitor {
    private var level = 0

    private fun printIndented(text: Any = "") {
        print(" ".repeat(indentation * level) + "$text")
    }

    private fun getType(type: ExprType): String {
        return when (type) {
            INT -> "int"
            BOOLEAN -> "boolean"
            VOID -> "void"
            is ARRAY -> "[".repeat(type.depth) + getType(type.innerExpr) + "]".repeat(type.depth)
            is CLASS -> type.className
        }
    }

    override fun preStmtVisit() {
        printIndented()
    }

    override fun postStmtVisit() {
        println()
    }

    override fun midVisit(arithExpr: ArithExpr) {
        print(" ${arithExpr.op.value} ")
    }

    override fun preVisit(arrayExpr: ArrayExpr) {
        print("[")
    }

    override fun midVisit(arrayExpr: ArrayExpr) {
        print(", ")
    }

    override fun postVisit(arrayExpr: ArrayExpr) {
        print("]")
    }

    override fun preVisit(arrayIndexExpr: ArrayIndexExpr) {
        print(arrayIndexExpr.id)
    }

    override fun preMidVisit(arrayIndexExpr: ArrayIndexExpr) {
        print("[")
    }

    override fun postMidVisit(arrayIndexExpr: ArrayIndexExpr) {
        print("]")
    }

    override fun preVisit(block: Block) {
        println("{")
        level++
    }

    override fun postVisit(block: Block) {
        level--
        printIndented("}")
    }

    override fun visit(booleanExpr: BooleanExpr) {
        print(booleanExpr.value)
    }

    override fun preVisit(booleanOpExpr: BooleanOpExpr) {
        if (booleanOpExpr.rhs == null) {
            print(booleanOpExpr.op.value)
        }
    }

    override fun midVisit(booleanOpExpr: BooleanOpExpr) {
        if (booleanOpExpr.rhs != null) {
            print(" ${booleanOpExpr.op.value} ")
        }
    }

    override fun preVisit(classDecl: ClassDecl) {
        printIndented("class ${classDecl.id}")
        if (classDecl.constructor.isNotEmpty()) {
            print("(${classDecl.constructor.joinToString(", ") { "${it.first}: ${getType(it.second)}" }})")
        }
        print(": ${classDecl.superclass}(")
        if (!classDecl.superclassArgs.isNullOrEmpty()) {
            classDecl.superclassArgs.dropLast(1).map {
                it.accept(this)
                print(", ")
            }
            classDecl.superclassArgs.last().accept(this)
        }
        println(") {")
        level++
    }

    override fun postVisit(classDecl: ClassDecl) {
        level--
        printIndented("}\n\n")
    }

    override fun visit(classField: ClassField) {
        print("${classField.objectId}.${classField.fieldId}")
    }

    override fun midVisit(compExpr: CompExpr) {
        print(" ${compExpr.op.value} ")
    }

    override fun preVisit(fieldDecl: FieldDecl) {
        print("var ")
        fieldDecl.ids.forEach { print("$it = ") }
    }

    override fun preVisit(funcCall: FuncCall) {
        print("${funcCall.id}(")
    }

    override fun midVisit(funcCall: FuncCall) {
        print(", ")
    }

    override fun postVisit(funcCall: FuncCall) {
        print(")")
    }

    override fun preVisit(funcDecl: FuncDecl) {
        println()
        printIndented("func ${funcDecl.id}(")
        print(funcDecl.params.joinToString(", ") { "${it.first}: ${getType(it.second)}" }) // Params
        println("): ${getType(funcDecl.returnType)} {")
        level++
    }

    override fun postVisit(funcDecl: FuncDecl) {
        level--
        printIndented("}\n")
    }

    override fun visit(idExpr: IdExpr) {
        print(idExpr.id)
    }

    override fun preVisit(ifElse: IfElse) {
        print("if (")
    }

    override fun preMidVisit(ifElse: IfElse) {
        print(") ")
    }

    override fun postMidVisit(ifElse: IfElse) {
        if (ifElse.elseBlock != null) print(" else ")
    }

    override fun visit(intExpr: IntExpr) {
        print(intExpr.value)
    }

    override fun preVisit(lenExpr: LenExpr) {
        print("len(")
    }

    override fun postVisit(lenExpr: LenExpr) {
        print(")")
    }

    override fun preVisit(methodCall: MethodCall) {
        print("${methodCall.objectId}.${methodCall.methodId}(")
    }

    override fun midVisit(methodCall: MethodCall) {
        print(", ")
    }

    override fun postVisit(methodCall: MethodCall) {
        print(")")
    }

    override fun preVisit(objectInstantiation: ObjectInstantiation) {
        print("${objectInstantiation.classId}(")
    }

    override fun midVisit(objectInstantiation: ObjectInstantiation) {
        print(", ")
    }

    override fun postVisit(objectInstantiation: ObjectInstantiation) {
        print(")")
    }

    override fun preVisit(parenExpr: ParenExpr) {
        print("(")
    }

    override fun postVisit(parenExpr: ParenExpr) {
        print(")")
    }

    override fun preVisit(printStmt: PrintStmt) {
        print("print(")
    }

    override fun postVisit(printStmt: PrintStmt) {
        print(")")
        if (printStmt.expr == null) println()
    }

    override fun preVisit(program: Program) {
        println("Main Scope {")
        level++
    }

    override fun postVisit(program: Program) {
        level--
        printIndented("}")
        println()
    }

    override fun preVisit(returnStmt: ReturnStmt) {
        print("return")
        if (returnStmt.expr != null) print(" ")
    }

    override fun visit(thisExpr: ThisExpr) {
        print("this")
    }

    override fun preVisit(varAssign: VarAssign) {
        varAssign.ids.forEach { print("$it = ") }
    }

    override fun midVisit(varAssign: VarAssign) {
        print(" = ")
    }

    override fun preVisit(varDecl: VarDecl) {
        print("var ")
        varDecl.ids.forEach { print("$it = ") }
    }

    override fun preVisit(whileLoop: WhileLoop) {
        print("while (")
    }

    override fun midVisit(whileLoop: WhileLoop) {
        print(") ")
    }
}