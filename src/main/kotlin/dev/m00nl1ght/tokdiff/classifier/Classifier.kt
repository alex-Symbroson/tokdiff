package dev.m00nl1ght.tokdiff.classifier

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult.Chunk
import dev.m00nl1ght.tokdiff.classifier.regex.BehaviourByRegex
import dev.m00nl1ght.tokdiff.classifier.regex.CategoryByRegex

class Classifier {

    companion object {

        const val lenient = false

        private const val cabrv = "[\\p{L}\\p{M}.]"
        private const val abbrev = "$cabrv+[0-9\\p{Lu}]$cabrv*"
        private const val word = "[\\p{L}\\p{M}]+"
        private const val wordnum = "[\\p{L}\\p{M}\\p{N}\\p{S}&&[^~\"]]+"
        private const val any = "[\\p{L}\\p{M}\\p{N}\\p{S}\\p{Pd}\\p{Pc}\\p{Po}&&[^~\"`']]+"

        fun root(): Category =
            Category("root",
                Category("abbrev",
                    CategoryByRegex("separated_abbrev",
                        BehaviourByRegex("split_sep", 1f,
                            basePattern = "$cabrv+~[0-9\\p{Lu}]$cabrv*",
                            endPattern = "~-~$word"
                        ),
                        BehaviourByRegex("split_sep", 1f,
                            basePattern = "$cabrv+~[0-9\\p{Lu}]$cabrv*",
                            endPattern = "-$word"
                        ),
                        BehaviourByRegex("split_sep_del", 1f,
                            basePattern = "$cabrv+~[0-9\\p{Lu}]$cabrv{0,4}",
                            endPattern = "~$word"
                        ),
                        BehaviourByRegex("kept", 1f,
                            basePattern = "$cabrv+[0-9\\p{Lu}]$cabrv*",
                            endPattern = "-$word"
                        ),
                        BehaviourByRegex("split", 1f,
                            basePattern = "$cabrv+[0-9\\p{Lu}]$cabrv*",
                            endPattern = "~-~$word"
                        ),
                        BehaviourByRegex("sep_del", 1f,
                            basePattern = "$cabrv+[0-9\\p{Lu}]$cabrv*",
                            endPattern = "~$word"
                        ),
                        BehaviourByRegex("cursed", 1f,
                            basePattern = "$cabrv+~?[0-9\\p{Lu}]$cabrv*",
                            endPattern = "~?-~?$word"
                        ),
                        segCheck = false
                    ),
                    CategoryByRegex("abbrev",
                        BehaviourByRegex("split_trailing_artifact", -1f,
                            basePattern = "$cabrv+~[0-9\\p{Lu}]$cabrv*~-"
                        ),
                        BehaviourByRegex("kept_trailing_artifact", -1f,
                            basePattern = "$cabrv+[0-9\\p{Lu}]$cabrv*~-"
                        ),
                        BehaviourByRegex("kept_artifact", 1f,
                            basePattern = "$cabrv+[0-9\\p{Lu}]$cabrv*-"
                        ),
                        BehaviourByRegex("split_artifact", -1f,
                            basePattern = "$cabrv+~[0-9\\p{Lu}]$cabrv*-"
                        ),
                        BehaviourByRegex("kept", 1f,
                            basePattern = "$cabrv+[0-9\\p{Lu}]$cabrv*"
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "$cabrv+~[0-9\\p{Lu}]$cabrv*"
                        ),
                        segCheck = false
                    )
                ),
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
                            tokenLimit = { s -> s },
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
                        BehaviourByRegex("separated", 1f,
                            basePattern = "\"~($any)~\""
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "\"($any)\""
                        ),
                        BehaviourByRegex("transformed", -1f,
                            basePattern = "``~($any)~''"
                        ),
                        BehaviourByRegex("separated_artifact", -1f,
                            basePattern = "\"~$any",
                            repeatingPattern = "~$any",
                            endPattern = "~\""
                        ),
                        BehaviourByRegex("dangling_artifact", -2f,
                            basePattern = "\"$any",
                            repeatingPattern = "~$any",
                            endPattern = "\""
                        ),
                        BehaviourByRegex("transformed_artifact", -2f,
                            basePattern = "``~$any",
                            repeatingPattern = "~$any",
                            endPattern = "~''"
                        )
                    ),
                    CategoryByRegex("quoted_phrase_start",
                        BehaviourByRegex("separated", 1f,
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
                        BehaviourByRegex("separated_split_trailing", 1f,
                            basePattern = "($any)~\"~,"
                        ),
                        BehaviourByRegex("separated_trailing", 1f,
                            basePattern = "($any)~\","
                        ),
                        BehaviourByRegex("separated", 1f,
                            basePattern = "($any)~\""
                        ),
                        BehaviourByRegex("dangling_split_trailing", -1f,
                            basePattern = "($any)\"~,"
                        ),
                        BehaviourByRegex("dangling_trailing", -1f,
                            basePattern = "($any)\","
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "($any)\""
                        ),
                        BehaviourByRegex("transformed_split_trailing", -1f,
                            basePattern = "($any)~''~,"
                        ),
                        BehaviourByRegex("transformed_trailing", -1f,
                            basePattern = "($any)~'',"
                        ),
                        BehaviourByRegex("transformed", -1f,
                            basePattern = "($any)~''"
                        )
                    )
                ),
                Category("paranthesis",
                    CategoryByRegex("parenthesized_word",
                        BehaviourByRegex("separated", 1f,
                            basePattern = "\\(~($any)~\\)"
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "\\(($any)\\)"
                        )
                    ),
                    CategoryByRegex("parenthesized_phrase_start",
                        BehaviourByRegex("separated", 1f,
                            basePattern = "\\(~($any)"
                        ),
                        BehaviourByRegex("dangling", -1f,
                            basePattern = "\\(($any)"
                        )
                    ),
                    CategoryByRegex("parenthesized_phrase_end",
                        BehaviourByRegex("separated", 1f,
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
                        BehaviourByRegex("split", -1f,
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
                        BehaviourByRegex("split", -1f,
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
                        BehaviourByRegex("split", -1f,
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
                        BehaviourByRegex("kept_trailing_sep", 1f,
                            basePattern = "([0-9]{1,3}).([0-9]{3})",
                            repeatingPattern = ".([0-9]{3})",
                            endPattern = "."
                        ),
                        BehaviourByRegex("kept_trailing", 1f,
                            basePattern = "([0-9]{1,3}).([0-9]{3})",
                            repeatingPattern = ".([0-9]{3})",
                            endPattern = "~."
                        ),
                        BehaviourByRegex("kept", 1f,
                            basePattern = "([0-9]{1,3}).([0-9]{3})",
                            repeatingPattern = ".([0-9]{3})"
                        ),
                        BehaviourByRegex("split_trailing_sep", 1f,
                            basePattern = "([0-9]{1,3})~.~([0-9]{3})",
                            repeatingPattern = "~.~([0-9]{3})",
                            endPattern = "."
                        ),
                        BehaviourByRegex("split_trailing", 1f,
                            basePattern = "([0-9]{1,3})~.~([0-9]{3})",
                            repeatingPattern = "~.~([0-9]{3})",
                            endPattern = "~."
                        ),
                        BehaviourByRegex("split", -1f,
                            basePattern = "([0-9]{1,3})~.~([0-9]{3})",
                            repeatingPattern = "~.~([0-9]{3})"
                        ),
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
                            basePattern = "([0-9]+)~.",
                            repeatingPattern = "([0-9]+)~.?"
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

            results.forEach { r ->
                r.category.occurences++
                r.chunks.forEach { c ->
                    c.value.occurencesByInput.merge(c.input.name, 1, Int::plus)
                }
            }
            return results
        }
    }

}
