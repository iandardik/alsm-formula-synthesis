package cmu.s3d.syn

import edu.mit.csail.sdg.alloy4.A4Reporter
import edu.mit.csail.sdg.parser.CompModule
import edu.mit.csail.sdg.parser.CompUtil
import edu.mit.csail.sdg.translator.A4Options
import edu.mit.csail.sdg.translator.A4Solution
import edu.mit.csail.sdg.translator.A4TupleSet
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod
import java.io.File
import java.lang.Exception

class FormulaVisitor(
    private val rawFile : String,
    private val world : CompModule,
    private val solution : A4Solution,
    private val tlaStyle : Boolean,
    private val formula : FormulaInfo
) {
    fun createString(alloyNode : String) : String {
        val childrenStr = "${alloyNode}.children"
        val childrenExpr = CompUtil.parseOneExpression_fromString(world, childrenStr)
        val children = (solution.eval(childrenExpr) as A4TupleSet)
            .map { it.atom(0) }
            .map { createString(it) }
            .toList()

        val nodeName = alloyNode.split("$")[0]
        val nodeType = if (nodeName.contains("Fluent")) "Fluent" else nodeName
        return when (nodeType) {
            "Root" -> children.joinToString("")
            "Not" -> "~(${children.joinToString("")})"
            "And" -> "(${children.joinToString(") /\\ (")})"
            "Implies" -> {
                val left = createString(queryAlloyModel("${alloyNode}.left", ""))
                val right = createString(queryAlloyModel("${alloyNode}.right", ""))
                "($left) => ($right)"
            }
            "Or" -> "(${children.joinToString(") \\/ (")})"
            //"EqualsVar" -> "(${children.joinToString(" = ")})"
            "VarEquals" -> {
                val lhs = queryAlloyModel("${alloyNode}.lhs", "").split("$")[0]
                val rhs = queryAlloyModel("${alloyNode}.rhs", "").split("$")[0]
                "$lhs = $rhs"
            }
            "VarNotEquals" -> {
                val lhs = queryAlloyModel("${alloyNode}.lhs", "").split("$")[0]
                val rhs = queryAlloyModel("${alloyNode}.rhs", "").split("$")[0]
                "$lhs # $rhs"
            }
            "VarSetContains" -> {
                val elem = queryAlloyModel("${alloyNode}.elem", "").split("$")[0]
                val theSet = queryAlloyModel("${alloyNode}.theSet", "").split("$")[0]
                "$elem \\in $theSet"
            }
            "VarLTE" -> {
                val lhs = queryAlloyModel("${alloyNode}.lhs", "").split("$")[0]
                val rhs = queryAlloyModel("${alloyNode}.rhs", "").split("$")[0]
                "$lhs <= $rhs"
            }
            "OnceAct" -> {
                val rawAct = queryAlloyModel("${alloyNode}.act", "")
                val act = rawAct.split("$")[0]
                if (tlaStyle) {
                    "once$act"
                } else {
                    "once($act)"
                }
            }
            "OnceVar" -> {
                val rawBase = queryAlloyModel("${alloyNode}.baseName", "")
                val base = rawBase.split("$")[0]

                val paramSeparator = if (tlaStyle) "][" else ","
                val strNumParams = queryAlloyModelDirectStr("#$alloyNode.vars")
                val numParams = strNumParams.toInt()
                val params = (0 until numParams)
                    .map { queryAlloyModel("P$it.($alloyNode.vars)", "") }
                    .map { it.split("$")[0] }
                    .joinToString(paramSeparator)
                if (tlaStyle) {
                    "once$base[$params]"
                } else {
                    "once($base($params))"
                }
            }
            "Fluent" -> {
                val curNumFluents = rawFile.split("\n")[0].replace("//", "").toInt()
                val fluentNum = alloyNode.split("$")[1].toInt()
                val newFluentNum = curNumFluents + fluentNum
                val fluentNodeName = alloyNode.split("$")[0] + newFluentNum

                // first, construct the fluent
                val numParams = queryAlloyModelDirectStr("#${alloyNode}.vars").toInt()
                val paramTypes = (0 until numParams)
                    .map { queryAlloyModel("P$it.($alloyNode.vars).envVarTypes", "") }
                    .map { it.split("$")[0] }

                val alloyFluentInitially = queryAlloyModel("${alloyNode}.initially", "")
                val fluentInitially = if (alloyFluentInitially.contains("True")) "TRUE" else "FALSE"

                val parseFluentAction = { symAct : String ->
                        val baseName = queryAlloyModel("${symAct}.baseName", "")
                            .replace(Regex("\\$.*$"), "")
                        val paramMappingPairs = queryAlloyModel("${symAct}.actToFlParamsMap")
                            .map { actIdx ->
                                val flIdx = queryAlloyModel("$actIdx.($symAct.actToFlParamsMap)", "")
                                val actIdxInt = actIdx
                                    .replace(Regex("\\$.*$"), "")
                                    .replace("P","")
                                    .toInt()
                                val flIdxInt = flIdx
                                    .replace(Regex("\\$.*$"), "")
                                    .replace("P","")
                                    .toInt()
                                Pair(actIdxInt,flIdxInt)
                            }
                        var paramMappings = mutableListOf<Int>()
                        for (i in paramMappingPairs.indices) {
                            val iMapping = paramMappingPairs.filter { it.second == i }
                            if (iMapping.size != 1) {
                                //println(paramMappingPairs)
                                error("invalid iMapping size at index $i (should be 1): ${iMapping.size}")
                            }
                            val mapping = iMapping[0]
                            paramMappings.add(mapping.first)
                        }

                        Pair(baseName, paramMappings)
                    }
                val fluentInitFl = queryAlloyModel("${alloyNode}.initFl")
                    .map(parseFluentAction)
                val fluentTermFl = queryAlloyModel("${alloyNode}.termFl")
                    .map(parseFluentAction)
                val fluentMutInitFl = try {
                        queryAlloyModel("${alloyNode}.mutInitFl").map(parseFluentAction)
                    } catch (e : Exception) {
                        emptyList<Pair<String, List<Int>>>()
                    }
                val fluentFalsifyFl = try {
                    queryAlloyModel("${alloyNode}.falsifyFl").map(parseFluentAction)
                } catch (e : Exception) {
                    emptyList<Pair<String, List<Int>>>()
                }
                /*val fluentMutTermFl = try {
                        queryAlloyModel("${alloyNode}.mutTermFl").map(parseFluentAction)
                    } catch (e : Exception) {
                        emptyList<Pair<String, List<Int>>>()
                    }*/
                val fluent = Fluent(paramTypes, fluentInitially, fluentInitFl, fluentTermFl, fluentMutInitFl, fluentFalsifyFl)
                formula.fluents[fluentNodeName] = fluent

                // second, return the formula
                val paramSeparator = if (tlaStyle) "][" else ","
                val params = (0 until numParams)
                    .map { queryAlloyModel("P$it.($alloyNode.vars)", "") }
                    .map { it.split("$")[0] }
                    .joinToString(paramSeparator)
                if (tlaStyle) {
                    "$fluentNodeName[$params]"
                } else {
                    "$fluentNodeName($params)"
                }
            }
            "Forall" -> {
                val rawVar = queryAlloyModel("${alloyNode}.var", "")
                val vr = rawVar.split("$")[0]
                val rawSort = queryAlloyModel("${alloyNode}.sort", "")
                val sort = rawSort.split("$")[0]
                "\\A $vr \\in $sort : ${children.joinToString("")}"
            }
            "Exists" -> {
                val rawVar = queryAlloyModel("${alloyNode}.var", "")
                val vr = rawVar.split("$")[0]
                val rawSort = queryAlloyModel("${alloyNode}.sort", "")
                val sort = rawSort.split("$")[0]
                "\\E $vr \\in $sort : ${children.joinToString("")}"
            }
            "TT" -> "TRUE"
            "FF" -> "FALSE"
            "DummyChild" -> "DummyChild"
            else -> error("Invalid node type: $nodeType")
        }
    }

    private fun queryAlloyModel(query : String) : List<String> {
        val expr = CompUtil.parseOneExpression_fromString(world, query)
        return (solution.eval(expr) as A4TupleSet).map { it.atom(0) }
    }

    private fun queryAlloyModel(query : String, sep : String) : String {
        return queryAlloyModel(query).joinToString(sep)
    }

    private fun queryAlloyModelDirectStr(query : String) : String {
        val expr = CompUtil.parseOneExpression_fromString(world, query)
        return solution.eval(expr) as String
    }
}

fun alloyOptions(custSolver : A4Options.SatSolver = A4Options.SatSolver.SAT4JMax): A4Options {
    return A4Options().apply {
        solver = custSolver
        skolemDepth = 1
        noOverflow = false
        inferPartialInstance = true
    }
}

fun alloyColdStart() {
    val reporter = A4Reporter.NOP
    val world = CompUtil.parseEverything_fromString(reporter, "")
    val options = alloyOptions()
    val command = world.allCommands.first()
    TranslateAlloyToKodkod.execute_command(reporter, world.allReachableSigs, command, options)
}

object AlsSynthesis {
    fun synthFormulaFromAls(path : String, tlaStyle : Boolean) : FormulaInfo {
        val als = File(path).readText()

        alloyColdStart()
        val reporter = A4Reporter.NOP
        val world = CompUtil.parseEverything_fromString(reporter, als)
        val options = alloyOptions(A4Options.SatSolver.OpenWBO)
        val command = world.allCommands.first()
        val solution = TranslateAlloyToKodkod.execute_command(reporter, world.allReachableSigs, command, options)

        if (!solution.satisfiable()) {
            return FormulaInfo("could not synthesize a formula! (UNSAT)")
        }

        for (a in solution.allAtoms) {
            world.addGlobal(a.label, a)
        }
        for (a in solution.allSkolems) {
            world.addGlobal(a.label, a)
        }

        val formulaInfo = FormulaInfo()
        val formula = FormulaVisitor(als, world, solution, tlaStyle, formulaInfo).createString("Root")
        formulaInfo.setFormula(formula)
        return formulaInfo
    }
}

