package dev.m00nl1ght.tokdiff

import dev.m00nl1ght.tokdiff.diff.MyersDiffAlgorithm
import dev.m00nl1ght.tokdiff.diff.MyersDiffOperation

class TokenChain(val name: String, val tokens: List<String>, val include: Boolean = true) {
    val displayName: String
        get() = name.replace("_", " ")
}

class DiffChunk(val begins: IntArray, val ends: IntArray)

fun calculateDiffs(inputs: List<TokenChain>): List<DiffChunk> {
    val cptr = IntArray(inputs.size) // current moving index in each token chain (moves ahead)
    val bptr = IntArray(inputs.size) // index in each token chain from the end of the previous iteration
    var mptr: IntArray? = null // marked index in each token chain (begin of current chunk)

    val diffs = ArrayList<DiffChunk>()
    val baseIdx = inputs.indices.first { i -> inputs[i].include }
    val base = inputs[baseIdx]

    val ops = inputs.map { other ->
        if (other == base) emptySequence<MyersDiffOperation<String>>().iterator()
        else MyersDiffAlgorithm(base.tokens, other.tokens).generateDiff().iterator()
    }

    while (cptr[baseIdx] < base.tokens.size) {

        var delta = 0
        for (i in inputs.indices) {
            if (i == baseIdx) continue
            val it = ops[i]
            while (it.hasNext()) {
                when (it.next()) {
                    is MyersDiffOperation.Insert<*> -> {
                        if (inputs[i].include) delta++
                        cptr[i]++
                    }
                    is MyersDiffOperation.Delete -> {
                        if (inputs[i].include) delta++
                        break
                    }
                    is MyersDiffOperation.Skip -> {
                        cptr[i]++
                        break
                    }
                }
            }
        }

        if (mptr == null && delta > 0) {
            mptr = bptr.copyOf()
        } else if (mptr != null && delta == 0) {
            val eptr = bptr.copyOf()
            for (i in eptr.indices) eptr[i] -= 2
            diffs.add(DiffChunk(mptr, eptr))
            mptr = null
        }

        cptr[baseIdx]++
        cptr.copyInto(bptr)
    }

    return diffs
}
