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
    val init : List<Pair<String,List<Int>>>,
    val term : List<Pair<String,List<Int>>>,
    val mutInit : List<Pair<String,List<Int>>>,
    val falsify : List<Pair<String,List<Int>>>
) {

    fun toJson() : String {
        val paramTypeStr = "[" + paramTypes.joinToString(",") { "\"$it\"" } + "]";
        val initStr = init
            .map { "{\"${it.first}\":[${it.second.joinToString(",")}]}" }
            .joinToString(",")
        val termStr = term
            .map { "{\"${it.first}\":[${it.second.joinToString(",")}]}" }
            .joinToString(",")
        val mutInitStr = mutInit
            .map { "{\"${it.first}\":[${it.second.joinToString(",")}]}" }
            .joinToString(",")
        val falsifyStr = falsify
            .map { "{\"${it.first}\":[${it.second.joinToString(",")}]}" }
            .joinToString(",")
        return "{\"paramTypes\":$paramTypeStr,\"initially\":\"$initially\"," +
                "\"init\":[$initStr],\"term\":[$termStr],\"mutInit\":[$mutInitStr],\"falsify\":[$falsifyStr]}"
    }
}