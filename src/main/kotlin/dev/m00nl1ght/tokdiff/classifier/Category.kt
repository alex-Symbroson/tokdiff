package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain

open class Category(val name: String, open vararg val sub: Category) {

    var occurences = 0

    val totalOccurences: Int
        get() = occurences + sub.sumOf { c -> c.totalOccurences }

    open fun evaluate(inputs: List<TokenChain>, diff: DiffChunk): ClassifierResult? {
        return sub.firstNotNullOfOrNull { c -> c.evaluate(inputs, diff) }
    }

    fun forEachCategory(depth: Int = 0, func: (category: Category, depth: Int) -> Unit) {
        func.invoke(this, depth)
        sub.forEach { c -> c.forEachCategory(depth + 1, func) }
    }
}
