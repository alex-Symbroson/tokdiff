package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain
import kotlin.math.min

open class Category(val name: String, open vararg val sub: Category) {

    var occurences = 0

    val totalOccurences: Int
        get() = occurences + sub.sumOf { c -> c.totalOccurences }

    open fun evaluate(inputs: List<TokenChain>, diff: DiffChunk): List<EvaluationResult> {
        return sub.map { c -> c.evaluate(inputs, diff) } .flatten()
    }

    fun forEachCategory(depth: Int = 0, func: (category: Category, depth: Int) -> Unit) {
        func.invoke(this, depth)
        sub.forEach { c -> c.forEachCategory(depth + 1, func) }
    }

    abstract class Base(
        name: String,
        val minTokenCount: Int = 1,
        val maxTokenCount: Int = 10
    ) : Category(name) {

        override fun evaluate(inputs: List<TokenChain>, diff: DiffChunk): List<EvaluationResult> {
            var results: MutableList<EvaluationResult>? = null

            for (i in inputs.indices) {
                val input = inputs[i]
                if (!input.include) continue
                val begin = diff.begins[i]
                val end = diff.ends[i]
                val length = end - begin + 1
                val maxOffset = length - minTokenCount

                try {
                    var offset = 0
                    while (offset <= maxOffset) {
                        val oBegin = begin + offset
                        val oEnd = min(end, oBegin + maxTokenCount - 1)
                        if (oEnd >= oBegin) {
                            val result = tryMatch(input, oBegin, oEnd)
                            if (result != null) {
                                results = results ?: ArrayList()
                                results += result
                                offset += result.length
                            }
                        }
                        offset++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return emptyList()
                }
            }

            return results ?: emptyList()
        }

        abstract fun tryMatch(input: TokenChain, begin: Int, end: Int): EvaluationResult?
    }

    class SeparatedWord(
        name: String,
        val delimeter: Char,
        minTokenCount: Int = 3,
        maxTokenCount: Int = 12
    ) : Base(name, minTokenCount, maxTokenCount) {

        override fun tryMatch(input: TokenChain, begin: Int, end: Int): EvaluationResult? {
            for (i in begin..end) {
                val token = input.tokens[i]

                if (i % 2 == begin % 2) {
                    if (token.length > 1) continue
                } else {
                    if (token.length == 1 && token[0] == delimeter) continue
                }
                if (i - begin < minTokenCount) return null
                return EvaluationResult(this, input, begin, i - 1)
            }
            return EvaluationResult(this, input, begin, end)
        }
    }

    class SeparatedNumber(
        name: String,
        val delimeter: Char,
        minTokenCount: Int = 1,
        maxTokenCount: Int = 10,
        val minSegLength: Int = 1,
        val maxSegLength: Int = 10,
        val lenientFirst: Boolean = false,
        val lenientLast: Boolean = false
    ) : Base(name, minTokenCount, maxTokenCount) {

        override fun tryMatch(input: TokenChain, begin: Int, end: Int): EvaluationResult? {
            for (i in begin..end) {
                val token = input.tokens[i]

                if (i % 2 == begin % 2) {
                    if (!token.all { char -> char.isDigit() }) return null
                    if (token.length in minSegLength..maxSegLength
                        || (lenientFirst && i == begin)
                        || (lenientLast && i == end)) continue
                } else {
                    if (token.length == 1 && token[0] == delimeter) continue
                }
                if (i - begin < minTokenCount) return null
                return EvaluationResult(this, input, begin, i - 1)
            }
            return EvaluationResult(this, input, begin, end)
        }
    }

    class QuotedSegment(
        name: String,
        val quote: Char,
        minTokenCount: Int = 3,
        maxTokenCount: Int = 3
    ) : Base(name, minTokenCount, maxTokenCount) {

        override fun tryMatch(input: TokenChain, begin: Int, end: Int): EvaluationResult? {
            val b = input.tokens[begin]
            val e = input.tokens[end]
            if (b.length != 1 || b[0] != quote) return null
            if (e.length != 1 || e[0] != quote) return null
            return EvaluationResult(this, input, begin, end)
        }
    }

    class TokenStartsWith(
        name: String,
        val prefix: String,
        minTokenCount: Int = 1,
        maxTokenCount: Int = 1
    ) : Base(name, minTokenCount, maxTokenCount) {

        override fun tryMatch(input: TokenChain, begin: Int, end: Int): EvaluationResult? {
            if (!input.tokens[begin].startsWith(prefix)) return null
            return EvaluationResult(this, input, begin, begin)
        }
    }

    class TokenEndsWith(
        name: String,
        val postfix: String,
        minTokenCount: Int = 1,
        maxTokenCount: Int = 1
    ) : Base(name, minTokenCount, maxTokenCount) {

        override fun tryMatch(input: TokenChain, begin: Int, end: Int): EvaluationResult? {
            if (!input.tokens[begin].endsWith(postfix)) return null
            return EvaluationResult(this, input, begin, begin)
        }
    }
}
