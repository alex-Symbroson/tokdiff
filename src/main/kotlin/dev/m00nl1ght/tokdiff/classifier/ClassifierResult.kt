package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.TokenChain
import kotlin.math.max

class ClassifierResult(val category: Category, val chunks: List<Chunk>) {
    class Chunk (
        val input: TokenChain,
        val value: Behaviour,
        val segments: List<String>,
        val begin: Int,
        val end: Int
    ) {
        val length: Int
            get() = max(end - begin + 1, 0)
    }
}


