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

class Fluent(
    val paramTypes : List<String>,
    val initially : String,
    val init : List<String>,
    val term : List<String>,
    val symActions : Map<String, List<Int>>
) {

    fun toJson() : String {
        val paramTypeStr = "[" + paramTypes.joinToString(",") { "\"$it\"" } + "]";
        val initStr = "[${init.joinToString(",") { "\"$it\"" }}]"
        val termStr = "[${term.joinToString(",") { "\"$it\"" }}]"
        val symActParamMaps = symActions
            .map { "\"${it.key}\":[${it.value.joinToString(",")}]" }
            .joinToString(",")
        return "{\"paramTypes\":$paramTypeStr,\"initially\":\"$initially\",\"init\":$initStr,\"term\":$termStr,\"symActParamMaps\":{$symActParamMaps}}"
    }
}