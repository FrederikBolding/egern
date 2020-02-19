package com.egern.codegen

import com.egern.ast.*
import com.egern.symbols.Symbol
import com.egern.symbols.SymbolTable
import com.egern.visitor.Visitor

class CodeGenerationVisitor(var symbolTable: SymbolTable) : Visitor {
    val instructions = ArrayList<Instruction>()


    companion object {
        // CONSTANT OFFSETS FROM RBP
        const val LOCAL_VAR_OFFSET = 1
        const val RETURN_OFFSET = -1
        const val STATIC_LINK_OFFSET = -2
        const val PARAM_OFFSET = -3
    }

    private fun add(instruction: Instruction) {
        instructions.add(instruction)
    }

    private fun followStaticLink(diff: Int) {
        add(
            Instruction(
                InstructionType.MOV, InstructionArg(RBP, Direct), InstructionArg(StaticLink, Direct),
                comment = "Prepare to follow static link pointer"
            )
        )
        for (i in 0..diff) {
            add(
                Instruction(
                    InstructionType.MOV,
                    InstructionArg(StaticLink, IndirectRelative(STATIC_LINK_OFFSET)),
                    InstructionArg(StaticLink, Direct),
                    comment = "Following static link pointer"
                )
            )
        }
    }

    override fun preVisit(program: Program) {
        add(Instruction(InstructionType.LABEL, InstructionArg(Memory("main"), Direct)))
        //TODO CALLER PROLOGUE?
    }

    override fun postVisit(program: Program) {
        //TODO CALLER EPILOGUE?
    }

    override fun preVisit(funcDecl: FuncDecl) {
        symbolTable = funcDecl.symbolTable
        add(
            Instruction(
                InstructionType.LABEL,
                InstructionArg(Memory(funcDecl.startLabel), Direct)
            )
        )

    }

    override fun postVisit(funcDecl: FuncDecl) {
        symbolTable = funcDecl.symbolTable.parent!!
        add(
            Instruction(
                InstructionType.LABEL,
                InstructionArg(Memory(funcDecl.endLabel), Direct)
            )
        )
    }

    override fun postVisit(funcCall: FuncCall) {
        // TODO: Handle parameters
        val func = symbolTable.lookup(funcCall.id)!!
        val decl = func.info as FuncDecl
        add(Instruction(InstructionType.CALL, InstructionArg(Memory(decl.startLabel), Direct)))
    }

    override fun preVisit(block: Block) {
        symbolTable = block.symbolTable
    }

    override fun postVisit(block: Block) {
        symbolTable = block.symbolTable.parent!!
    }

    override fun visit(intExpr: IntExpr) {
        add(
            Instruction(
                InstructionType.PUSH,
                InstructionArg(ImmediateValue(intExpr.value.toString()), Direct),
                comment = "Push static integer value"
            )
        )
    }

    override fun visit(idExpr: IdExpr) {
        val symbol = symbolTable.lookup(idExpr.id) ?: throw Exception("Symbol ${idExpr.id} is undefined")
        val scopeDiff = symbolTable.scope - symbol.scope
        followStaticLink(scopeDiff)
    }

    override fun postVisit(compExpr: CompExpr) {
        // Pop expressions to register 1 and 2
        add(
            Instruction(
                InstructionType.POP,
                InstructionArg(Register(RegisterKind.OpReg2), Direct),
                comment = "Pop expression to register 2"
            )
        )
        add(
            Instruction(
                InstructionType.POP,
                InstructionArg(Register(RegisterKind.OpReg1), Direct),
                comment = "Pop expression to register 1"
            )
        )
        add(
            Instruction(
                InstructionType.CMP,
                InstructionArg(Register(RegisterKind.OpReg2), Direct),
                InstructionArg(Register(RegisterKind.OpReg1), Direct),
                comment = "Compare with ${compExpr.op.value}"
            )
        )
        val trueLabel = LabelGenerator.nextLabel("cmp_true")
        val endLabel = LabelGenerator.nextLabel("cmp_end");
        val jumpOperator = when (compExpr.op) {
            CompOp.EQ -> InstructionType.JE
            CompOp.NEQ -> InstructionType.JNE
            CompOp.LT -> InstructionType.JL
            CompOp.GT -> InstructionType.JG
            CompOp.LTE -> InstructionType.JLE
            CompOp.GTE -> InstructionType.JGE
        }
        add(Instruction(jumpOperator, InstructionArg(Memory(trueLabel), Direct), comment = "Jump if true"))
        add(
            Instruction(
                InstructionType.PUSH,
                InstructionArg(ImmediateValue("0"), Direct),
                comment = "Push false if comparison was false"
            )
        )
        add(
            Instruction(
                InstructionType.JMP,
                InstructionArg(Memory(endLabel), Direct),
                comment = "Skip pushing false if success"
            )
        )
        add(
            Instruction(
                InstructionType.LABEL,
                InstructionArg(Memory(trueLabel), Direct)
            )
        )
        add(
            Instruction(
                InstructionType.PUSH,
                InstructionArg(ImmediateValue("1"), Direct),
                comment = "Push true if comparison was true"
            )
        )
        add(
            Instruction(
                InstructionType.LABEL,
                InstructionArg(Memory(endLabel), Direct)
            )
        )
    }

    override fun postVisit(arithExpr: ArithExpr) {
        // Pop expressions to register 1 and 2
        add(
            Instruction(
                InstructionType.POP,
                InstructionArg(Register(RegisterKind.OpReg1), Direct),
                comment = "Pop expression to register 1"
            )
        )
        add(
            Instruction(
                InstructionType.POP,
                InstructionArg(Register(RegisterKind.OpReg2), Direct),
                comment = "Pop expression to register 2"
            )
        )
        val arithOperator = when (arithExpr.op) {
            ArithOp.PLUS -> InstructionType.ADD
            ArithOp.MINUS -> InstructionType.SUB
            ArithOp.TIMES -> InstructionType.IMUL
            ArithOp.DIVIDE -> InstructionType.IDIV
        }
        add(
            Instruction(
                arithOperator,
                InstructionArg(Register(RegisterKind.OpReg1), Direct),
                InstructionArg(Register(RegisterKind.OpReg2), Direct),
                comment = "Do arithmetic operation"
            )
        )
        add(
            Instruction(
                InstructionType.PUSH,
                InstructionArg(Register(RegisterKind.OpReg2), Direct),
                comment = "Push result to stack"
            )
        )
    }

    override fun postVisit(printStmt: PrintStmt) {
        add(Instruction(InstructionType.META, MetaOperation.CallerSave))
        add(Instruction(InstructionType.META, MetaOperation.Print))
        add(Instruction(InstructionType.META, MetaOperation.CallerRestore))
    }

    override fun postVisit(returnStmt: ReturnStmt) {
        if (returnStmt.expr != null) {
            add(
                Instruction(
                    InstructionType.POP,
                    InstructionArg(ReturnValue, Direct),
                    comment = "Pop expression to return value register"
                )
            )
        }
        add(Instruction(InstructionType.RET))
    }

    override fun preMidVisit(ifElse: IfElse) {
        add(
            Instruction(
                InstructionType.POP,
                InstructionArg(Register(RegisterKind.OpReg1), Direct),
                comment = "Pop expression to register"
            )
        )
        add(
            Instruction(
                InstructionType.MOV,
                InstructionArg(ImmediateValue("1"), Direct),
                InstructionArg(Register(RegisterKind.OpReg2), Direct),
                comment = "Move true to other register"
            )
        )
        add(
            Instruction(
                InstructionType.CMP,
                InstructionArg(Register(RegisterKind.OpReg1), Direct),
                InstructionArg(Register(RegisterKind.OpReg2), Direct),
                comment = "Compare the expression to true"
            )
        )
        if (ifElse.elseBlock != null) {
            add(
                Instruction(
                    InstructionType.JNE,
                    InstructionArg(Memory(ifElse.elseLabel), Direct),
                    comment = "Jump to optional else part"
                )
            )
        }
    }

    override fun postMidVisit(ifElse: IfElse) {
        add(
            Instruction(
                InstructionType.JMP,
                InstructionArg(Memory(ifElse.endLabel), Direct),
                comment = "Skip else part if successful"
            )
        )
        add(
            Instruction(
                InstructionType.LABEL,
                InstructionArg(Memory(ifElse.elseLabel), Direct)
            )
        )
    }

    override fun postVisit(ifElse: IfElse) {
        add(
            Instruction(
                InstructionType.LABEL,
                InstructionArg(Memory(ifElse.endLabel), Direct)
            )
        )
    }

    override fun postVisit(varAssign: VarAssign<*>) {
        val symbols = ArrayList<Symbol<*>?>()
        for (id in varAssign.ids) {
            symbols.add(symbolTable.lookup(id))
        }
        add(Instruction(InstructionType.POP, InstructionArg(Register(RegisterKind.DataReg), Direct)))
        for (symbol in symbols) {
            // TODO: Move value to all places where symbols are stored
        }
    }

    // TODO: Generate code
}
