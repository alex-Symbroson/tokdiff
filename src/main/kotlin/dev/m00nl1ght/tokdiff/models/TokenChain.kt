package dev.m00nl1ght.tokdiff.models

class TokenChain(val source: String, val tokens: List<String>, val include: Boolean) {

    val eval: Array<EvaluationResult?> = arrayOfNulls(tokens.size)

    fun firstEvalIn(begin: Int, end: Int): Int {
        for (i in begin..end) if (eval[i] != null) return i
        return -1
    }

    fun putEval(result: EvaluationResult) {
        for (i in result.begin..result.end) eval[i] = result
    }
}
