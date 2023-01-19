package dev.m00nl1ght.tokdiff.export

import dev.m00nl1ght.tokdiff.TokenChain
import dev.m00nl1ght.tokdiff.classifier.Classifier
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult
import dev.m00nl1ght.tokdiff.classifier.regex.CategoryByRegex
import dev.m00nl1ght.tokdiff.export.WorkbookRefs.Style.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xddf.usermodel.chart.*
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFDrawing
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean
import java.io.File
import java.io.FileOutputStream

class WorkbookWriter(val workbook: WorkbookRefs = WorkbookRefs(XSSFWorkbook())) {

    companion object {
        var formatDisplayNames = false
    }

    val String.forDisplay: String
        get() = if (formatDisplayNames) this.replace("_", " ") else this

    fun saveWorkbook(baseDir: File, baseName: String, number: Int) {
        val outputFile = File(baseDir, if (number >= 0) "$baseName-$number.xlsx" else "$baseName.xlsx")
        println("Saving workbook to: ${outputFile.absolutePath}")
        val outputStream = FileOutputStream(outputFile)
        workbook.root.write(outputStream)
        workbook.root.close()
    }

    fun makeSheet(name: String) {
        workbook.sheet = workbook.root.createSheet(name)
        workbook.clearMark().reset()
    }

    fun writeDiffs(name: String, inputs: List<TokenChain>, results: List<ClassifierResult>) {
        val inputIdx = inputs.indexOfFirst { c -> c.name == "input" }

        workbook.width(20).row(Header)
        workbook.put(name, Header).y++
        workbook.y++

        workbook.mark()

        if (inputIdx >= 0) {
            workbook.put("Position", Header).y++
            workbook.put("Context", Header).y++
        }

        workbook.put("Category", Header).y++
        workbook.y++

        for (tokenChain in inputs) {
            if (tokenChain.include) {
                workbook.put(tokenChain.name.forDisplay, Header).y++
            }
        }

        workbook.reset().x++

        for (result in results) {

            if (inputIdx >= 0) {
                val chain = inputs[inputIdx]
                val max = chain.tokens.size - 1
                val begin = result.chunks[inputIdx].begin
                val end = Integer.max(result.chunks[inputIdx].end, begin)
                val beginExt = Integer.max(begin - 1, 0)
                val endExt = Integer.min(end + 1, max)
                val prefix = if (beginExt > 0) "[...] " else ""
                val postfix = if (endExt < max) " [...]" else ""
                val pstr = if (begin == end) "$begin" else "$begin - $end"
                val context = IntRange(beginExt, endExt)
                    .joinToString(" ", prefix, postfix) { i -> chain.tokens[i] }

                workbook.put(pstr, Subheader).x++
                workbook.cell(Subheader)
                workbook.x--
                workbook.y++

                workbook.put(context, Subheader).x++
                workbook.cell(Subheader)
                workbook.x--
                workbook.y++
            }

            workbook.put(result.category.name, Subheader).x++
            workbook.cell(Subheader)
            workbook.x--
            workbook.y += 2

            val uniqueBuf = ArrayList<String>()
            for ((i, input) in inputs.withIndex()) {
                if (!input.include) continue

                val chunk = result.chunks[i]
                val str = IntRange(chunk.begin, chunk.end)
                    .joinToString(" | ", "", "") { t -> input.tokens[t] }

                var uidx = uniqueBuf.indexOf(str)
                if (uidx < 0) {
                    uidx = uniqueBuf.size
                    uniqueBuf.add(str)
                }

                val style = workbook.colorStyleFor(uidx)
                workbook.cell().cellStyle = style
                workbook.put(str).x++
                workbook.cell().cellStyle = style
                workbook.put("[" + chunk.value.name.forDisplay + "]").y++
                workbook.x--
            }

            workbook.width(40).x++
            workbook.width(15).x++
            workbook.resetY()
        }

        workbook.reset().y += inputs.size + 4
    }

    fun writeCategorySummary(totalDiffs: Int) {
        workbook.width(30).put("Category", Header).x++
        workbook.width(20).put("Occurences", Header)
        workbook.resetX().y++

        Classifier.root.forEachCategory { category, depth ->
            if (depth == 2) {
                workbook.put(category.name).x++
                workbook.put(category.totalOccurences.toDouble(), decimalFormat = false)
                workbook.resetX().y++
            }
        }

        workbook.put(Classifier.unknown.name.forDisplay).x++
        workbook.put(Classifier.unknown.totalOccurences.toDouble(), decimalFormat = false)
        workbook.resetX().mark().y++

        workbook.put("total occurences", Subheader).x++
        workbook.put(Classifier.root.totalOccurences.toDouble(), Subheader, false)
        workbook.resetX().y++

        workbook.put("total diff chunks", Subheader).x++
        workbook.put(totalDiffs.toDouble(), Subheader, false)
        workbook.resetX().y++

        workbook.applyConditionalFormatting(CellRangeAddress(1, workbook.y, 1, 1), false)

        makeChart(
            "Occurences",
            CellRangeAddress(0, 25, 3, 13),
            CellRangeAddress(1, workbook.my, 0, 0),
            CellRangeAddress(1, workbook.my, 1, 1),
            barDirection = BarDirection.BAR
        )
    }

    fun writeBehaviourSummary(inputNames: Collection<String>) {
        workbook.row(Header)
        workbook.width(30).put("Behaviour", Header).x++
        workbook.width(16).put("Count", Header).x++
        workbook.width(16).x++

        for (inputName in inputNames) {
            workbook.width(16).put(inputName.forDisplay, Header).x++
        }

        val xmax = workbook.x
        workbook.resetX().y++

        Classifier.root.forEachCategory { category, _ ->
            if (category is CategoryByRegex) {
                workbook.row(Subheader)
                workbook.put(category.name.forDisplay, Subheader).x++
                workbook.put(category.totalOccurences.toDouble(), Subheader, false).x++
                workbook.resetX().y++

                for (behaviour in category.behaviours) {
                    val count = behaviour.occurencesByInput.values.reduceOrNull(Int::plus) ?: 0
                    workbook.put(behaviour.name).x++
                    workbook.put(count.toDouble(), decimalFormat = false)
                    workbook.x += 2

                    for (inputName in inputNames) {
                        val countForInput = behaviour.occurencesByInput.getOrDefault(inputName, 0)
                        workbook.put(countForInput.toDouble(), decimalFormat = false).x++
                    }

                    workbook.resetX().y++
                }
            }
        }

        workbook.applyConditionalFormatting(CellRangeAddress(1, workbook.y, 1, xmax), false)
    }

    fun writeScoreSummary(inputNames: Collection<String>) {
        workbook.row(Header)
        workbook.width(30).put("Behaviour", Header).x++
        workbook.width(16).put("Score", Header).x++
        workbook.width(16).x++

        val dataStartX = workbook.x
        for (inputName in inputNames) {
            workbook.width(16).put(inputName.forDisplay, Header).x++
        }

        workbook.resetX().y++

        val catRows = LinkedHashMap<CategoryByRegex, Int>()

        Classifier.root.forEachCategory { category, _ ->
            if (category is CategoryByRegex) {
                workbook.row(Subheader)
                workbook.put(category.name, Subheader).x++

                val countRef = CellReference("Behaviours", workbook.y, workbook.x, false, false).formatAsString()
                workbook.x += 2

                if (category.behaviours.isNotEmpty()) {
                    for (inputName in inputNames) {
                        val firstRef = CellReference(workbook.y + 1, workbook.x).formatAsString()
                        val lastRef = CellReference(workbook.y + category.behaviours.size, workbook.x).formatAsString()
                        workbook.putFormula("SUM($firstRef:$lastRef)", Subheader)
                        workbook.x++
                    }
                }

                catRows[category] = workbook.y
                workbook.resetX().y++

                for (behaviour in category.behaviours) {
                    workbook.put(behaviour.name.forDisplay).x++
                    val scoreRef = workbook.cellref.formatAsString()
                    workbook.put(behaviour.score.toDouble())
                    workbook.x += 2

                    for (inputName in inputNames) {
                        val bCountRef = CellReference("Behaviours", workbook.y, workbook.x, false, false).formatAsString()
                        workbook.putFormula("($bCountRef*$scoreRef)/MAX(1,$countRef)")
                        workbook.x++
                    }

                    workbook.resetX().y++
                }
            }
        }

        workbook.row(Header)
        workbook.put("total", Header).x++
        workbook.x += 2

        for (inputName in inputNames) {
            val refs = catRows.values.joinToString(",") { r -> CellReference(r, workbook.x).formatAsString() }
            workbook.putFormula("SUM($refs)", Header)
            workbook.x++
        }

        workbook.applyConditionalFormatting(CellRangeAddress(1, workbook.y, 1, workbook.x), true)

        val totalY = workbook.y
        workbook.x = 1
        workbook.y += 2

        makeChart(
            "Total Scores",
            CellRangeAddress(workbook.y, workbook.y + 20, workbook.x, workbook.x + 5),
            CellRangeAddress(0, 0, dataStartX, dataStartX + inputNames.size - 1),
            CellRangeAddress(totalY, totalY, dataStartX, dataStartX + inputNames.size - 1),
            barDirection = BarDirection.COL
        )

        var eol = true

        for (row in catRows) {
            if (row.key.totalOccurences <= 0) continue

            eol = !eol
            if (eol) {
                workbook.x = 1
                workbook.y += 20
            } else {
                workbook.x += 5
            }

            makeChart(
                "Scores for ${row.key.name}",
                CellRangeAddress(workbook.y, workbook.y + 20, workbook.x, workbook.x + 5),
                CellRangeAddress(0, 0, dataStartX, dataStartX + inputNames.size - 1),
                CellRangeAddress(row.value, row.value, dataStartX, dataStartX + inputNames.size - 1),
                barDirection = BarDirection.COL
            )
        }
    }

    private fun makeChart(
        title: String,
        chartRect: CellRangeAddress,
        categoryRange: CellRangeAddress,
        vararg dataRanges: CellRangeAddress,
        barDirection: BarDirection = BarDirection.BAR
    ) {
        val drawing: XSSFDrawing = workbook.sheet!!.createDrawingPatriarch()
        val anchor: XSSFClientAnchor = workbook.anchor(chartRect)

        val chart = drawing.createChart(anchor)
        chart.ctChartSpace.roundedCorners = CTBoolean.Factory.newInstance()
        chart.ctChartSpace.roundedCorners.setVal(false)
        chart.setTitleText(title)
        chart.titleOverlay = false

        val leftAxis = chart.createValueAxis(AxisPosition.LEFT)
        leftAxis.crossBetween = AxisCrossBetween.BETWEEN
        leftAxis.crosses = AxisCrosses.AUTO_ZERO

        val bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM)
        bottomAxis.crosses = AxisCrosses.AUTO_ZERO
        bottomAxis.tickLabelPosition = AxisTickLabelPosition.LOW

        val chartData = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis) as XDDFBarChartData
        chartData.barDirection = barDirection

        val categorySource = XDDFDataSourcesFactory.fromStringCellRange(workbook.sheet, categoryRange)

        for ((index, dataRange) in dataRanges.withIndex()) {
            val dataSource = XDDFDataSourcesFactory.fromNumericCellRange(workbook.sheet, dataRange)
            val series = chartData.addSeries(categorySource, dataSource) as XDDFBarChartData.Series
            series.setTitle(title, null)
            series.invertIfNegative = false
            series.setShowLeaderLines(true)

            val ser = chart.ctChart.plotArea.getBarChartArray(0).getSerArray(index)
            ser.dLbls.addNewShowVal().setVal(true)
            ser.dLbls.addNewShowLegendKey().setVal(false)
            ser.dLbls.addNewShowCatName().setVal(false)
            ser.dLbls.addNewShowSerName().setVal(false)
        }

        chartData.setVaryColors(true)
        chart.plot(chartData)
    }

}
