package dev.m00nl1ght.tokdiff

import dev.m00nl1ght.tokdiff.classifier.Classifier
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult
import dev.m00nl1ght.tokdiff.export.WorkbookWriter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

fun main(args: Array<String>) {

    val baseDir = File(if (args.isNotEmpty()) args[0] else ".")
    val maxIdx = if (args.size > 1) args[1].toInt() else -1
    val writeDiffs = if (args.size > 2) args[2].toBoolean() else true

    val tokFiles = ArrayList<ZipFile>()
    val inputFiles = ArrayList<ZipFile>()
    val inputNames = LinkedHashSet<String>()

    var workbookWriter = WorkbookWriter()
    var outputFileNumber = 0

    val filter: (result: ClassifierResult) -> Boolean = when {
        args.size > 3 -> { r -> r.category.name == args[3] }
        else -> { _ -> true }
    }

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

        val entryNames = tokFiles
            .flatMap { obj -> obj.entries().asSequence() }
            .map { obj -> obj.name }
            .distinct().toList()

        var totalDiffs = 0

        if (writeDiffs) {
            workbookWriter.makeSheet("Diff")
        }

        for ((idx, entryName) in entryNames.withIndex()) {

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
                        inputNames.add(name)
                    }
                } catch (e: Exception) {
                    println("Failed to read entry $entryName from ${zipFile.name}")
                }
            }

            try {
                val diffChunks = calculateDiffs(tokenChains)
                totalDiffs += diffChunks.size
                val results = diffChunks
                    .map { c -> Classifier.evaluate(tokenChains, c) }
                    .flatten().filter(filter).toList()
                if (writeDiffs && results.isNotEmpty()) workbookWriter.writeDiffs(entryName, tokenChains, results)
            } catch (e: Exception) {
                println("Failed to process diffs for input $entryName")
                e.printStackTrace()
            }

            if (maxIdx != -1 && idx > maxIdx) break
            if (idx % 100 == 0) {
                println("Finished input $idx of ${entryNames.size}")
            }

            if ((idx + 1) % 1000 == 0 && writeDiffs) {
                workbookWriter.saveWorkbook(baseDir, "diffs", outputFileNumber)
                println("Finished processing input $idx, starting new output file")
                workbookWriter = WorkbookWriter()
                workbookWriter.makeSheet("Diff")
                outputFileNumber++
            }
        }

        workbookWriter.saveWorkbook(baseDir, "diffs", outputFileNumber)
        println("Finished processing all inputs, starting summary output file")

        workbookWriter = WorkbookWriter()
        workbookWriter.makeSheet("Categories")
        workbookWriter.writeCategorySummary(totalDiffs)
        workbookWriter.makeSheet("Behaviours")
        workbookWriter.writeBehaviourSummary(inputNames)
        workbookWriter.makeSheet("Scores")
        workbookWriter.writeScoreSummary(inputNames)
        workbookWriter.saveWorkbook(baseDir, "summary", -1)

    } finally {
        tokFiles.forEach(ZipFile::close)
        inputFiles.forEach(ZipFile::close)
    }
}
