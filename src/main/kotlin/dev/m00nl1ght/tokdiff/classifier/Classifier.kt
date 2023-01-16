package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult.Chunk
import dev.m00nl1ght.tokdiff.classifier.regex.BehaviourByRegex
import dev.m00nl1ght.tokdiff.classifier.regex.CategoryByRegex

class Classifier {

    companion object {

        const val lenient = false

        private const val word = "[\\p{L}\\p{M}]+"
        private const val wordnum = "[\\p{L}\\p{M}\\p{N}\\p{S}&&[^~]]+"
        private const val any = "[\\p{L}\\p{M}\\p{N}\\p{S}\\p{Pd}\\p{Pc}\\p{Po}&&[^~]]+"

        fun root(): Category =
            Category("root",
                Category("separated_word",
                    CategoryByRegex("separated_word_interpunct",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "($word)·($word)",
                            repeatingPattern = "·($word)",
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "($word)~·~($word)",
                            repeatingPattern = "~·~($word)",
                        )
                    ),
                    CategoryByRegex("separated_word_hyphen",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "($wordnum)-($word)",
                            repeatingPattern = "-($word)",
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "($wordnum)~-~($word)",
                            repeatingPattern = "~-~($word)",
                        ),
                        BehaviourByRegex("consumed", -1f,
                            basePattern = "($wordnum)~($word)",
                            repeatingPattern = "~($word)",
                            initiator = false
                        )
                    ),
                    CategoryByRegex("separated_word_hyphen_open",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "($wordnum)-",
                            repeatingPattern = "($word)-",
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "($wordnum)~-",
                            repeatingPattern = "~($word)~-",
                        )
                    )
                ),
                Category("quoted",
                    CategoryByRegex("quoted_word",
                        BehaviourByRegex("separate", 1f,
                            basePattern = "\"~($any)~\""
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "\"($any)\""
                        ),
                        BehaviourByRegex("transformed", -1f,
                            basePattern = "``~($any)~''"
                        )
                    ),
                    CategoryByRegex("quoted_phrase_start",
                        BehaviourByRegex("separate", 1f,
                            basePattern = "\"~($any)"
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "\"($any)"
                        ),
                        BehaviourByRegex("transformed", -1f,
                            basePattern = "``~($any)"
                        )
                    ),
                    CategoryByRegex("quoted_phrase_end",
                        BehaviourByRegex("separate", 1f,
                            basePattern = "($any)~\""
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "($any)\""
                        ),
                        BehaviourByRegex("transformed", -1f,
                            basePattern = "($any)~''"
                        )
                    )
                ),
                Category("paranthesis",
                    CategoryByRegex("parenthesized_word",
                        BehaviourByRegex("separate", 1f,
                            basePattern = "\\(~($any)~\\)"
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "\\(($any)\\)"
                        )
                    ),
                    CategoryByRegex("parenthesized_phrase_start",
                        BehaviourByRegex("separate", 1f,
                            basePattern = "\\(~($any)"
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "\\(($any)"
                        )
                    ),
                    CategoryByRegex("parenthesized_phrase_end",
                        BehaviourByRegex("separate", 1f,
                            basePattern = "($any)~\\)"
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "($any)\\)"
                        )
                    )
                ),
                Category("date",
                    CategoryByRegex("date_full",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{2,4})"
                        ),
                        BehaviourByRegex("separate", -1f,
                            basePattern = "([0-9]{1,2})~\\.~([0-9]{1,2})~\\.~([0-9]{2,4})"
                        ),
                        BehaviourByRegex("dangling_pre", -1f,
                            basePattern = "([0-9]{1,2})\\.~([0-9]{1,2})\\.~([0-9]{2,4})"
                        ),
                        BehaviourByRegex("dangling_post", -1f,
                            basePattern = "([0-9]{1,2})~\\.([0-9]{1,2})~\\.([0-9]{2,4})"
                        )
                    ),
                    CategoryByRegex("date_partial",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "([0-9]{1,2})\\.([0-9]{1,2})\\."
                        ),
                        BehaviourByRegex("separate", -1f,
                            basePattern = "([0-9]{1,2})~\\.~([0-9]{1,2})~\\."
                        ),
                        BehaviourByRegex("dangling_pre", -1f,
                            basePattern = "([0-9]{1,2})\\.~([0-9]{1,2})\\."
                        ),
                        BehaviourByRegex("dangling_post", -1f,
                            basePattern = "([0-9]{1,2})~\\.([0-9]{1,2})\\."
                        )
                    )
                ),
                Category("time",
                    CategoryByRegex("time_full",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "([0-9]{1,2}):([0-9]{2})"
                        ),
                        BehaviourByRegex("separate", -1f,
                            basePattern = "([0-9]{1,2})~:~([0-9]{2})"
                        ),
                        BehaviourByRegex("dangling_pre", -1f,
                            basePattern = "([0-9]{1,2}):~([0-9]{2})"
                        ),
                        BehaviourByRegex("dangling_post", -1f,
                            basePattern = "([0-9]{1,2})~:([0-9]{2})"
                        )
                    )
                ),
                Category("number",
                    CategoryByRegex("number_separated",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "([0-9]{1,3}).([0-9]{3})",
                            repeatingPattern = ".([0-9]{3})"
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "([0-9]{1,3})~.~([0-9]{3})",
                            repeatingPattern = "~.~([0-9]{3})"
                        )
                    ),
                    CategoryByRegex("number_decimal_fraction",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "([0-9]+),([0-9]+)"
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "([0-9]+)~,~([0-9]+)"
                        )
                    ),
                    CategoryByRegex("number_then_dot",
                        BehaviourByRegex("kept", 1f,
                            basePattern = "([0-9]+)."
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "([0-9]+)~."
                        )
                    )
                ),
                Category("artefact",
                    CategoryByRegex("artefact",
                        BehaviourByRegex("xml_like", -1f,
                            basePattern = "&#$any;"
                        ),
                        BehaviourByRegex("json_like", -1f,
                            basePattern = "\\\\$wordnum"
                        ),
                        BehaviourByRegex("ignored", 0f,
                            initiator = false
                        )
                    )
                )
            )

        val root = root()
        val unknown = Category("unknown")

        val unidentified = Behaviour("unidentified", 0f)

        fun evaluate(inputs: List<TokenChain>, diffChunk: DiffChunk): List<ClassifierResult> {

            val results = ArrayList<ClassifierResult>()
            val includedIndexes = inputs.indices.filter { i -> inputs[i].include }
            val diff = DiffChunk(diffChunk.begins.copyOf(), diffChunk.ends.copyOf())

            var anyHasMore = true
            while (anyHasMore) {
                val result = root.evaluate(inputs, diff)
                if (result != null) {
                    results.add(result)
                    anyHasMore = false
                    for (i in includedIndexes) {
                        val next = result.chunks[i].end + 1
                        diff.begins[i] = next
                        if (next <= diff.ends[i]) anyHasMore = true
                    }
                } else {
                    if (includedIndexes.all { i -> diff.begins[i] <= diff.ends[i] }) {
                        val tokens = includedIndexes.map { i -> inputs[i].tokens[diff.begins[i]] } .distinct()
                        if (tokens.size == 1) {
                            anyHasMore = false
                            for (i in includedIndexes) {
                                val next = diff.begins[i] + 1
                                diff.begins[i] = next
                                if (next <= diff.ends[i]) anyHasMore = true
                            }
                            continue
                        }
                    }

                    results.add(ClassifierResult(unknown, inputs.indices.map { i ->
                        Chunk(inputs[i], unidentified, emptyList(), diff.begins[i], diff.ends[i])
                    }.toList()))
                    break
                }
            }

            return results
        }
    }

}
