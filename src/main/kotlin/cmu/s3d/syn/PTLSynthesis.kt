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
            "Implies" -> {
                val left = createString(queryAlloyModel("${alloyNode}.left", ""))
                val right = createString(queryAlloyModel("${alloyNode}.right", ""))
                "($left => $right)"
            }
            "Or" -> "(${children.joinToString(" \\/ ")})"
            "EqualsVar" -> "(${children.joinToString(" = ")})"
            "OnceAct" -> {
                val rawAct = queryAlloyModel("${alloyNode}.oact", "")
                val act = rawAct.split("$")[0]
                "once($act)"
            }
            "OnceVar" -> {
                val rawBase = queryAlloyModel("${alloyNode}.base", "")
                val base = rawBase.split("$")[0]

                val strNumParams = queryAlloyModelDirectStr("#$alloyNode.ovars")
                val numParams = strNumParams.toInt()
                val params = (0 until numParams)
                    .map { queryAlloyModel("$alloyNode.ovars.subseq[$it,$it].first", "") }
                    .map { it.replace("$", "") }
                    .joinToString(",")
                "once($base($params))"
            }
            "Forall" -> {
                val rawVar = queryAlloyModel("${alloyNode}.var", "")
                val vr = rawVar.replace("$", "")
                val rawSort = queryAlloyModel("${alloyNode}.sort", "")
                val sort = rawSort.split("$")[0]
                "\\A $vr \\in $sort : ${children.joinToString("")}"
            }
            "TT" -> "TRUE"
            "FF" -> "FALSE"
            else -> error("Invalid node type: $nodeType")
        }
    }

    private fun queryAlloyModel(query : String, sep : String) : String {
        val expr = CompUtil.parseOneExpression_fromString(world, query)
        return (solution.eval(expr) as A4TupleSet).joinToString(sep) { it.atom(0) }
    }

    private fun queryAlloyModelDirectStr(query : String) : String {
        val expr = CompUtil.parseOneExpression_fromString(world, query)
        return solution.eval(expr) as String
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







