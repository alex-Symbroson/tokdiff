@file:Suppress("ArrayInDataClass")

package dev.m00nl1ght.tokdiff

import dev.m00nl1ght.tokdiff.diff.MyersDiffAlgorithm
import dev.m00nl1ght.tokdiff.diff.MyersDiffOperation
import dev.m00nl1ght.tokdiff.diff.MyersDiffOperation.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Integer.max
import java.lang.Integer.min
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.zip.ZipFile

data class TokenChain(val source: String, val tokens: List<String>, val include: Boolean)
data class DiffChunk(val begins: IntArray, val ends: IntArray)
data class DiffResults(val name: String, val inputs: List<TokenChain>, val diffs: List<DiffChunk>)

fun main(args: Array<String>) {

    val baseDir = File(if (args.isNotEmpty()) args[0] else "run/nl")
    val outputFile = File(baseDir, "output.xlsx")

    val tokFiles = ArrayList<ZipFile>()
    val inputFiles = ArrayList<ZipFile>()

    val workbook = WorkbookRefs(XSSFWorkbook())
    workbook.sheet = workbook.root.createSheet("Diff")

    try {

        baseDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".zip")) {
                if (file.name.startsWith("input")) {
                    inputFiles += ZipFile(file)
                } else {
                    tokFiles += ZipFile(file)
                }
            }
        }

        val entryNames = tokFiles.stream()
            .flatMap { obj -> obj.stream() }
            .map { obj -> obj.name }
            .distinct().toList()

        for (entryName in entryNames) {

            val tokenChains = ArrayList<TokenChain>()
            val inputEntryName = entryName.substringBeforeLast('.') + ".txt"

            for (zipFile in inputFiles) {
                try {
                    val entry = zipFile.getEntry(inputEntryName)
                    if (entry != null) {
                        val instream = zipFile.getInputStream(entry)
                        val text = String(instream.readAllBytes(), StandardCharsets.UTF_8)
                        val tokens = applyBasicTokenizer(text)
                        tokenChains.add(TokenChain("input", tokens, false))
                        break
                    }
                } catch (e: Exception) {
                    println("Failed to read entry $inputEntryName from ${zipFile.name}")
                }
            }

            for (zipFile in tokFiles) {
                try {
                    val entry = zipFile.getEntry(entryName)
                    if (entry != null) {
                        val instream = zipFile.getInputStream(entry)
                        val text = String(instream.readAllBytes(), StandardCharsets.UTF_8)
                        val tokens = parseTokens(text)
                        val name = zipFile.name.substringAfterLast(File.separatorChar).substringBeforeLast('.')
                        tokenChains.add(TokenChain(name, tokens, true))
                    }
                } catch (e: Exception) {
                    println("Failed to read entry $entryName from ${zipFile.name}")
                }
            }

            val diffChunks = calculateDiffs(tokenChains)
            val results = DiffResults(entryName, tokenChains, diffChunks)
            writeDiffs(workbook, results)
        }
    } finally {
        tokFiles.forEach(ZipFile::close)
        inputFiles.forEach(ZipFile::close)
    }

    val outputStream = FileOutputStream(outputFile)
    println("Saving workbook to: ${outputFile.absolutePath}")
    workbook.root.write(outputStream)
    workbook.root.close()
}

fun applyBasicTokenizer(input: String): List<String> {
    return input.split(' ', '\n')
}

private fun writeDiffs(workbook: WorkbookRefs, data: DiffResults) {
    val inputIdx = data.inputs.indexOfFirst { c -> c.source == "input" }

    workbook.width(20).row().rowStyle = workbook.darkGreyHeaderStyle
    workbook.put(data.name, workbook.darkGreyHeaderStyle).y++
    workbook.y++

    workbook.mark()

    if (inputIdx >= 0) {
        workbook.put("Position", workbook.darkGreyHeaderStyle).y++
        workbook.put("Context", workbook.darkGreyHeaderStyle).y++
    }

    workbook.put("Category", workbook.darkGreyHeaderStyle).y++
    workbook.y++

    for (tokenChain in data.inputs) {
        if (tokenChain.include) {
            workbook.put(tokenChain.source, workbook.darkGreyHeaderStyle).y++
        }
    }

    workbook.reset().x++

    for (diffChunk in data.diffs) {

        if (inputIdx >= 0) {
            val chain = data.inputs[inputIdx]
            val max = chain.tokens.size - 1
            val begin = diffChunk.begins[inputIdx]
            val end = max(diffChunk.ends[inputIdx], begin)
            val beginExt = max(begin - 1, 0)
            val endExt = min(end + 1, max)
            val prefix = if (beginExt > 0) "[...] " else ""
            val postfix = if (endExt < max) " [...]" else ""
            val pstr = if (begin == end) "$begin" else "$begin - $end"
            val context = IntStream.rangeClosed(beginExt, endExt)
                .mapToObj { i -> chain.tokens[i] }
                .collect(Collectors.joining(" ", prefix, postfix))

            workbook.put(pstr, workbook.greyHeaderStyle).y++
            workbook.put(context, workbook.greyHeaderStyle).y++
        }

        workbook.put("Unknown", workbook.greyHeaderStyle).y++
        workbook.y++

        for (idx in data.inputs.indices) {
            val tokenChain = data.inputs[idx]
            if (!tokenChain.include) continue
            val str = IntStream.rangeClosed(diffChunk.begins[idx], diffChunk.ends[idx])
                .mapToObj { i -> tokenChain.tokens[i] }
                .collect(Collectors.joining(" | ", "", ""))
            workbook.put(str).y++
        }

        workbook.width(50)
        workbook.resetY().x++
    }

    workbook.reset().y += data.inputs.size + 5
}

private fun calculateDiffs(inputs: List<TokenChain>): List<DiffChunk> {
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
                    is Insert<*> -> {
                        if (inputs[i].include) delta++
                        cptr[i]++
                    }
                    is Delete -> {
                        if (inputs[i].include) delta++
                        break
                    }
                    is Skip -> {
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

