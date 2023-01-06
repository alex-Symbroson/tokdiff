package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain

open class Category(val name: String, open val sub: List<Category> = emptyList()) {

    open fun tryMatch(inputs: List<TokenChain>, diff: DiffChunk): Category? {
        return sub.firstNotNullOfOrNull { c -> c.tryMatch(inputs, diff) }
    }

    companion object {

        private val unknown = Category("unknown")
        private val errored = Category("errored")

        private val root = Category("root", listOf(
            Category("separated_word", listOf(
                SeparatedWord("separated_word_hyphen", '-'),
                SeparatedWord("separated_word_interpunct", '·'),
            )),
            Category("quoted", listOf(
                // TODO quoted_word -> '"text"'
                // TODO quoted_phrase -> '"text text text"'
            )),
            Category("number", listOf(
                // TODO number_separated -> '60.000.000'
                // TODO number_decimal_fraction -> '5,9'
                // TODO number_percent -> '67 %'
            )),
            Category("date", listOf(
                // TODO date_full -> '19.05.22' '19.05.2022'
                // TODO date_partial -> '19.05.'
            )),
            Category("time", listOf(
                // TODO time_full -> '20:30'
            )),
            Category("artefact", listOf(
                // TODO artefact_json_like -> '\t'
                // TODO artefact_xml_like -> '&#9;'
            )),
            unknown,
            errored
        ))

        fun evaluate(inputs: List<TokenChain>, diff: DiffChunk): Category {
            return try {
                root.tryMatch(inputs, diff) ?: unknown
            } catch (e: Exception) {
                e.printStackTrace()
                errored
            }
        }
    }

    class SeparatedWord(name: String, val delimeter: Char) : MatchAny(name) {

        override fun tryMatch(input: TokenChain, diffBegin: Int, diffEnd: Int): Category? {
            if (diffEnd - diffBegin < 2) return null
            for (i in diffBegin..diffEnd) {
                val token = input.tokens[i]

                if (i % 2 == diffBegin % 2) {
                    if (token.length > 1) continue
                } else {
                    if (token.length == 1 && token[0] == delimeter) continue
                }
                return null
            }
            return this
        }
    }

    abstract class MatchAny(name: String, override val sub: List<MatchAny> = emptyList()) : Category(name, sub) {

        override fun tryMatch(inputs: List<TokenChain>, diff: DiffChunk): Category? {
            return inputs.indices.firstNotNullOfOrNull { i -> tryMatch(inputs[i], diff.begins[i], diff.ends[i]) }
        }

        open fun tryMatch(input: TokenChain, diffBegin: Int, diffEnd: Int): Category? {
            return sub.firstNotNullOfOrNull { c -> c.tryMatch(input, diffBegin, diffEnd) }
        }
    }
}
