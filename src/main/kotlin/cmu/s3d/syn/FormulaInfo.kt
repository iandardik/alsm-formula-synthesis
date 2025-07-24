package cmu.s3d.syn

class FormulaInfo(
    private var formula : String = ""
) {
    val fluents : MutableMap<String, Fluent> = mutableMapOf()

    fun setFormula(f : String) {
        formula = f
    }

    fun toJson() : String {
        val fluentsStr = fluents
            .map { "\"${it.key}\":${it.value.toJson()}" }
            .joinToString(",")
        return "{" +
                    "\"formula\":\"${formula.replace("\\","\\\\")}\"," +
                    "\"fluents\": {$fluentsStr}" +
                "}"
    }

    override fun toString() : String {
        return formula
    }
}

class FlAction(
    val baseName : String,
    val priority : Int,
    val paramMap : List<Int>,
    val value : String
) {
    fun toJson() : String {
        return "{\"baseName\":\"${baseName}\",\"priority\":${priority},\"paramMap\":${paramMap},\"value\":\"$value\"}"
    }
}

class Fluent(
    val paramTypes : List<String>,
    val initially : String,
    val flActions : List<FlAction>
) {

    fun toJson() : String {
        val paramTypeStr = "[" + paramTypes.joinToString(",") { "\"$it\"" } + "]";
        val actFlStr = "[" + flActions.joinToString(",") { it.toJson() } + "]";
        return "{\"paramTypes\":$paramTypeStr,\"initially\":\"$initially\",\"actionFls\":$actFlStr}"
    }
}