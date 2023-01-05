@file:Suppress("ArrayInDataClass")

package dev.m00nl1ght.tokdiff

import dev.m00nl1ght.tokdiff.diff.MyersDiffAlgorithm
import dev.m00nl1ght.tokdiff.diff.MyersDiffOperation
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

data class TokenChain(val source: String, val tokens: List<String>)
data class DiffChunk(val begins: IntArray, val ends: IntArray)
data class DiffResults(val name: String, val inputs: List<TokenChain>, val diffs: List<DiffChunk>)

fun main(args: Array<String>) {

    val baseDir = File(if (args.isNotEmpty()) args[0] else "run")
    val inputDir = File(baseDir, "input")
    val zipFiles = ArrayList<ZipFile>()

    try {

        inputDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".zip")) zipFiles += ZipFile(file)
        }

        val entryNames = zipFiles.stream()
            .flatMap { obj -> obj.stream() }
            .map { obj -> obj.name }
            .distinct().toList()

        for (entryName in entryNames) {

            val tokenChains = ArrayList<TokenChain>()
            for (zipFile in zipFiles) {
                try {
                    val entry = zipFile.getEntry(entryName)
                    if (entry != null) {
                        val instream = zipFile.getInputStream(entry)
                        val text = String(instream.readAllBytes(), StandardCharsets.UTF_8)
                        val tokens = parseTokens(text)
                        tokenChains.add(TokenChain(zipFile.name, tokens))
                    }
                } catch (e: Exception) {
                    println("Failed to read entry $entryName from ${zipFile.name}")
                }
            }

            val diffChunks = calculateDiffs(tokenChains)
            val results = DiffResults(entryName, tokenChains, diffChunks)

            for (diff in results.diffs) {
                println("${diff.begins[0]} - ${diff.ends[0]} = ${tokenChains[0].tokens.subList(diff.begins[0], diff.ends[0])}")
            }

        }
    } finally {
        zipFiles.forEach(ZipFile::close)
    }
}

private fun calculateDiffs(inputs: List<TokenChain>): List<DiffChunk> {
    val diffs = ArrayList<DiffChunk>()
    val cptr = IntArray(inputs.size) // current moving index in each token chain (moves ahead)
    val bptr = IntArray(inputs.size) // index in each token chain from the end of the previous iteration

    val base = inputs[0]
    val ops = inputs.map { other ->
        if (other == base) emptySequence<MyersDiffOperation<String>>().iterator()
        else MyersDiffAlgorithm(base.tokens, other.tokens).generateDiff().iterator()
    }

    var mptr: IntArray? = null // marked index in each token chain (begin of current chunk)
    while (cptr[0] < base.tokens.size) {
        var delta = 0
        for (i in 1 until inputs.size) {
            val it = ops[i]
            while (it.hasNext()) {
                when (it.next()) {
                    is MyersDiffOperation.Insert<*> -> {
                        delta++
                        cptr[i]++
                    }
                    is MyersDiffOperation.Delete -> {
                        delta++
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
            diffs.add(DiffChunk(mptr, cptr.copyOf()))
            mptr = null
        }

        cptr.copyInto(bptr)
        cptr[0]++
    }

    return diffs
}

private fun parseTokens(input: String): List<String> {
    var begin = -1
    val list = ArrayList<String>()

    for (i in input.indices) {
        if (input[i] == '\'') {
            begin = if (begin < 0) i else {
                list.add(input.substring(begin + 1, i))
                -1
            }
        }
    }

    if (begin >= 0) throw RuntimeException("quote not closed from $begin")
    return list
}
