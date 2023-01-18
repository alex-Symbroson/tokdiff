package dev.m00nl1ght.tokdiff.classifier.regex

import dev.m00nl1ght.tokdiff.DiffChunk
import dev.m00nl1ght.tokdiff.TokenChain
import dev.m00nl1ght.tokdiff.classifier.Category
import dev.m00nl1ght.tokdiff.classifier.Classifier
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult.Chunk
import java.util.LinkedList

class CategoryByRegex(name: String, vararg val behaviours: BehaviourByRegex, val segCheck: Boolean = true) : Category(name) {

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

                if (segments.isNotEmpty()) segments = LinkedList<String>()

                if (b.basePattern != null) {
                    val matcher = b.basePattern.matcher(combStr)
                    if (!matcher.lookingAt()) continue
                    for (g in 1..matcher.groupCount()) segments.add(matcher.group(g))
                    combPos = matcher.end()
                }

                if (b.repeatingPattern != null) {
                    while (combPos < combStr.length) {
                        val matcher = b.repeatingPattern.matcher(combStr)
                        matcher.region(combPos, combStr.length)
                        if (!matcher.lookingAt()) break
                        for (g in 1..matcher.groupCount()) segments.add(matcher.group(g))
                        combPos = matcher.end()
                    }
                }

                if (b.endPattern != null) {
                    val matcher = b.endPattern.matcher(combStr)
                    matcher.region(combPos, combStr.length)
                    if (!matcher.lookingAt()) continue
                    for (g in 1..matcher.groupCount()) segments.add(matcher.group(g))
                    combPos = matcher.end()
                }

                val end = when (combPos) {
                    0 -> dBegin - 1
                    combStr.length -> dEnd
                    else -> {
                        if (combStr[combPos] != '~') continue
                        val occ = IntRange(0, combPos - 1).map { p -> combStr[p] }.filter { c -> c == '~' }.size
                        dBegin + occ
                    }
                }

                chunks.add(Chunk(input, b, segments, dBegin, end))
                continue@itinputs
            }

            if (Classifier.lenient) {
                chunks.add(Chunk(input, Classifier.unidentified, emptyList(), dBegin, dEnd))
            } else {
                return null
            }
        }

        val initiator = chunks.firstOrNull { c -> c.value is BehaviourByRegex && c.value.initiator } ?: return null

        for ((i, c) in chunks.withIndex()) {
            if (c.value is BehaviourByRegex) {
                if (c.value.initiator) {
                    if (segCheck && c.segments.size != initiator.segments.size) return null
                } else {
                    if (c.segments.size < initiator.segments.size) return null
                    val limit = c.value.tokenLimit(initiator.segments.size)
                    if (limit >= 0 && c.length > limit) {
                        chunks[i] = Chunk(c.input, c.value, c.segments.subList(0, limit), c.begin, c.begin + limit - 1)
                    }
                }
            }
        }

        for (i in initiator.segments.indices) {
            for (c in chunks) {
                if (c.value == Classifier.unidentified) continue
                if (segCheck && c.segments[i] != initiator.segments[i]) {
                    return null
                }
            }
        }

        return ClassifierResult(this, chunks)
    }

}
