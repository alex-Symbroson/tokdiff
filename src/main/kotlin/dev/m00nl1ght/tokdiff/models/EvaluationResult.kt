package dev.m00nl1ght.tokdiff.models

import dev.m00nl1ght.tokdiff.classifier.Category
import kotlin.math.max

data class EvaluationResult(
    val category: Category,
    val input: TokenChain,
    val begin: Int = -1,
    val end: Int = -1,
    val value: String = ""
) {
    val length: Int
        get() = max(end - begin + 1, 0)
}
