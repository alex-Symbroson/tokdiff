package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.models.DiffChunk
import dev.m00nl1ght.tokdiff.models.EvaluationResult
import dev.m00nl1ght.tokdiff.models.TokenChain
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

    companion object {

        val root = Category("root",
            Category("separated_word",
                SeparatedWord("separated_word_interpunct",
                    delimeter = '·'
                ),
                SeparatedWord("separated_word_hyphen",
                    delimeter = '-'
                ),
                SeparatedWord("separated_word_hyphen_open",
                    delimeter = '-',
                    minTokenCount = 2,
                    maxTokenCount = 2
                )
            ),
            Category("quoted",
                QuotedSegment("quoted_word",
                    quote = '"'
                ),
                TokenStartsWith("quoted_phrase",
                    prefix = "\""
                )
            ),
            Category("paranthesis",
                TokenStartsWith("paranthesis_start",
                    prefix = "(",
                    maxTokenCount = 10
                ),
                TokenEndsWith("paranthesis_end",
                    postfix = ")",
                    maxTokenCount = 10
                )
            ),
            Category("date",
                SeparatedNumber("date_full",
                    delimeter = '.',
                    minSegLength = 1,
                    maxSegLength = 2,
                    minTokenCount = 5,
                    maxTokenCount = 5,
                    lenientLast = true
                ),
                SeparatedNumber("date_partial",
                    delimeter = '.',
                    minSegLength = 1,
                    maxSegLength = 2,
                    minTokenCount = 4,
                    maxTokenCount = 4
                )
            ),
            Category("time",
                SeparatedNumber("time_full",
                    delimeter = ':',
                    minSegLength = 1,
                    maxSegLength = 2,
                    minTokenCount = 3,
                    maxTokenCount = 3
                )
            ),
            Category("number",
                SeparatedNumber("number_separated",
                    delimeter = '.',
                    minSegLength = 3,
                    maxSegLength = 3,
                    minTokenCount = 3,
                    lenientFirst = true
                ),
                SeparatedNumber("number_decimal_fraction",
                    delimeter = ',',
                    minTokenCount = 3,
                    maxTokenCount = 3
                ),
                SeparatedNumber("number_then_dot",
                    delimeter = '.',
                    minTokenCount = 2,
                    maxTokenCount = 2
                )
            ),
            Category("artefact",
                TokenStartsWith("artefact_xml",
                    prefix = "&#"
                ),
                TokenStartsWith("artefact_json",
                    prefix = "\\"
                )
            )
        )

        val unknown = Category("unknown")
        val errored = Category("errored")

        fun evaluate(inputs: List<TokenChain>, diff: DiffChunk): List<EvaluationResult> {
            val results = root.evaluate(inputs, diff)
            results.map { r -> r.category } .distinct().forEach { c -> c.occurences++ }
            return results.ifEmpty {
                unknown.occurences++
                listOf(EvaluationResult(unknown, inputs[0]))
            }
        }
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
                        val oFirstDone = input.firstEvalIn(oBegin, oEnd)
                        val oEndCapped = if (oFirstDone < 0) oEnd else (oFirstDone - 1)
                        if (oEndCapped >= oBegin) {
                            val result = tryMatch(input, oBegin, oEnd)
                            if (result != null) {
                                input.putEval(result)
                                results = results ?: ArrayList()
                                results += result
                                offset += result.length
                            }
                        }
                        offset++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val error = EvaluationResult(errored, input, begin, end)
                    input.putEval(error)
                    errored.occurences++
                    return listOf(error)
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
