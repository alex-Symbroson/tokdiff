package dev.m00nl1ght.tokdiff.classifier.regex

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain
import dev.m00nl1ght.tokdiff.classifier.Category
import dev.m00nl1ght.tokdiff.classifier.Classifier
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult.Chunk
import java.util.LinkedList

class CategoryByRegex(name: String, vararg val behaviours: BehaviourByRegex) : Category(name) {

    override fun evaluate(inputs: List<TokenChain>, diff: DiffChunk): ClassifierResult? {

        val chunks = LinkedList<Chunk>()
        var segments = LinkedList<String>()

        itinputs@ for ((i, input) in inputs.withIndex()) {

            val dBegin = diff.begins[i]
            val dEnd = diff.ends[i]

            if (!input.include) {
                chunks.add(Chunk(input, Classifier.unidentified, segments, dBegin, dEnd))
                continue
            }

            val combStr = IntRange(dBegin, dEnd).joinToString("~") { t -> input.tokens[t] }

            for (b in behaviours) {
                var combPos = 0
                var consumedTokens = 0

                if (segments.isNotEmpty()) segments = LinkedList<String>()

                if (b.basePattern != null) {
                    val matcher = b.basePattern.matcher(combStr)
                    if (!matcher.lookingAt()) continue
                    for (g in 1..matcher.groupCount()) segments.add(matcher.group(g))
                    combPos = matcher.end()
                    consumedTokens += b.baseTokenCount
                }

                if (b.repeatingPattern != null) {
                    while (combPos < combStr.length) {
                        val matcher = b.repeatingPattern.matcher(combStr)
                        matcher.region(combPos, combStr.length)
                        if (!matcher.lookingAt()) break
                        for (g in 1..matcher.groupCount()) segments.add(matcher.group(g))
                        combPos = matcher.end()
                        consumedTokens += b.repeatingTokenCount
                    }
                }

                // don't accept chunks that end in the middle of a token
                if (combPos != 0 && combPos != combStr.length && combStr[combPos] != '~') continue

                chunks.add(Chunk(input, b, segments, dBegin, dBegin + consumedTokens - 1))
                continue@itinputs
            }

            if (Classifier.lenient) {
                chunks.add(Chunk(input, Classifier.unidentified, emptyList(), dBegin, dEnd))
            } else {
                return null
            }
        }

        val initiator = chunks.firstOrNull { c -> c.value is BehaviourByRegex && c.value.initiator } ?: return null

        for (c in chunks) {
            if (c.value is BehaviourByRegex && c.value.initiator) {
                if (c.segments.size != initiator.segments.size) return null
            } else {
                if (c.value == Classifier.unidentified) continue
                if (c.segments.size < initiator.segments.size) return null
            }
        }

        for (i in initiator.segments.indices) {
            for (c in chunks) {
                if (c.value == Classifier.unidentified) continue
                if (c.segments[i] != initiator.segments[i]) return null
            }
        }

        return ClassifierResult(this, chunks)
    }

}
