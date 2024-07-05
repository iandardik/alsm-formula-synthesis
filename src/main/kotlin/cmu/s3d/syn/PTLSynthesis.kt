package cmu.s3d.syn

import edu.mit.csail.sdg.alloy4.A4Reporter
import edu.mit.csail.sdg.parser.CompModule
import edu.mit.csail.sdg.parser.CompUtil
import edu.mit.csail.sdg.translator.A4Options
import edu.mit.csail.sdg.translator.A4Solution
import edu.mit.csail.sdg.translator.A4TupleSet
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod
import java.io.File
import java.lang.RuntimeException

class FormulaVisitor(
    private val world : CompModule,
    private val solution : A4Solution
) {
    fun createString(alloyNode : String) : String {
        val childrenStr = "${alloyNode}.children"
        val childrenExpr = CompUtil.parseOneExpression_fromString(world, childrenStr)
        val children = (solution.eval(childrenExpr) as A4TupleSet)
            .map { it.atom(0) }
            .map { createString(it) }
            .toList()

        val nodeType = alloyNode.split("$")[0]
        return when (nodeType) {
            "Root" -> children.joinToString("")
            "Not" -> "~(${children.joinToString("")})"
            "And" -> "(${children.joinToString(" /\\ ")})"
            "Implies" -> "(${children.joinToString(" => ")})"
            "Or" -> "(${children.joinToString(" \\/ ")})"
            "OnceAct" -> {
                val actStr = "${alloyNode}.act"
                val actExpr = CompUtil.parseOneExpression_fromString(world, actStr)
                val rawAct = (solution.eval(actExpr) as A4TupleSet).joinToString("") { it.atom(0) }
                val act = rawAct.split("$")[0]
                "once($act)"
            }
            "TT" -> "TRUE"
            "FF" -> "FALSE"
            else -> error("Invalid node type: $nodeType")
        }
    }
}

fun alloyDefaultOptions(): A4Options {
    return A4Options().apply {
        solver = A4Options.SatSolver.SAT4JMax
        skolemDepth = 1
        noOverflow = false
        inferPartialInstance = true
    }
}

fun alloyColdStart() {
    val reporter = A4Reporter.NOP
    val world = CompUtil.parseEverything_fromString(reporter, "")
    val options = alloyDefaultOptions()
    val command = world.allCommands.first()
    TranslateAlloyToKodkod.execute_command(reporter, world.allReachableSigs, command, options)
}

fun synthFormulaFromAls(path : String) : String {
    val als = File(path).readText()

    alloyColdStart()
    val reporter = A4Reporter.NOP
    val world = CompUtil.parseEverything_fromString(reporter, als)
    val options = alloyDefaultOptions()
    val command = world.allCommands.first()
    val solution = TranslateAlloyToKodkod.execute_command(reporter, world.allReachableSigs, command, options)

    if (!solution.satisfiable()) {
        return "could not synthesize a formula! (UNSAT)"
    }

    for (a in solution.allAtoms) {
        world.addGlobal(a.label, a)
    }
    for (a in solution.allSkolems) {
        world.addGlobal(a.label, a)
    }

    return FormulaVisitor(world, solution).createString("Root")
}

/*
fun synthFormulaForWA(errLts : NFA<String, Integer>) : String {

    return ""
}
 */







