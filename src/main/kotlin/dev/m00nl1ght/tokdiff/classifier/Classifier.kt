package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain

class Classifier {

    companion object {

        val root = Category("root",
            Category("separated_word",
                Category.SeparatedWord(
                    "separated_word_interpunct",
                    delimeter = '·'
                ),
                Category.SeparatedWord(
                    "separated_word_hyphen",
                    delimeter = '-'
                ),
                Category.SeparatedWord(
                    "separated_word_hyphen_open",
                    delimeter = '-',
                    minTokenCount = 2,
                    maxTokenCount = 2
                )
            ),
            Category("quoted",
                Category.QuotedSegment(
                    "quoted_word",
                    quote = '"'
                ),
                Category.TokenStartsWith(
                    "quoted_phrase",
                    prefix = "\""
                )
            ),
            Category("paranthesis",
                Category.TokenStartsWith(
                    "paranthesis_start",
                    prefix = "(",
                    maxTokenCount = 10
                ),
                Category.TokenEndsWith(
                    "paranthesis_end",
                    postfix = ")",
                    maxTokenCount = 10
                )
            ),
            Category("date",
                Category.SeparatedNumber(
                    "date_full",
                    delimeter = '.',
                    minSegLength = 1,
                    maxSegLength = 2,
                    minTokenCount = 5,
                    maxTokenCount = 5,
                    lenientLast = true
                ),
                Category.SeparatedNumber(
                    "date_partial",
                    delimeter = '.',
                    minSegLength = 1,
                    maxSegLength = 2,
                    minTokenCount = 4,
                    maxTokenCount = 4
                )
            ),
            Category("time",
                Category.SeparatedNumber(
                    "time_full",
                    delimeter = ':',
                    minSegLength = 1,
                    maxSegLength = 2,
                    minTokenCount = 3,
                    maxTokenCount = 3
                )
            ),
            Category("number",
                Category.SeparatedNumber(
                    "number_separated",
                    delimeter = '.',
                    minSegLength = 3,
                    maxSegLength = 3,
                    minTokenCount = 3,
                    lenientFirst = true
                ),
                Category.SeparatedNumber(
                    "number_decimal_fraction",
                    delimeter = ',',
                    minTokenCount = 3,
                    maxTokenCount = 3
                ),
                Category.SeparatedNumber(
                    "number_then_dot",
                    delimeter = '.',
                    minTokenCount = 2,
                    maxTokenCount = 2
                )
            ),
            Category("artefact",
                Category.TokenStartsWith(
                    "artefact_xml",
                    prefix = "&#"
                ),
                Category.TokenStartsWith(
                    "artefact_json",
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

}
