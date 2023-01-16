package dev.m00nl1ght.tokdiff.export

import dev.m00nl1ght.tokdiff.classifier.Classifier
import dev.m00nl1ght.tokdiff.TokenChain
import dev.m00nl1ght.tokdiff.classifier.ClassifierResult
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xddf.usermodel.chart.*
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFDrawing
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean
import java.io.File
import java.io.FileOutputStream

class WorkbookWriter(val workbook: WorkbookRefs = WorkbookRefs(XSSFWorkbook())) {

    fun saveWorkbook(baseDir: File, baseName: String, number: Int) {
        val outputFile = File(baseDir, if (number >= 0) "$baseName-$number.xlsx" else "$baseName.xlsx")
        println("Saving workbook to: ${outputFile.absolutePath}")
        val outputStream = FileOutputStream(outputFile)
        workbook.root.write(outputStream)
        workbook.root.close()
    }

    fun makeSheet(name: String) {
        workbook.sheet = workbook.root.createSheet(name)
    }

    fun writeDiffs(name: String, inputs: List<TokenChain>, results: List<ClassifierResult>) {
        val inputIdx = inputs.indexOfFirst { c -> c.name == "input" }

        workbook.width(20).row().rowStyle = workbook.darkGreyHeaderStyle
        workbook.put(name, workbook.darkGreyHeaderStyle).y++
        workbook.y++

        workbook.mark()

        if (inputIdx >= 0) {
            workbook.put("Position", workbook.darkGreyHeaderStyle).y++
            workbook.put("Context", workbook.darkGreyHeaderStyle).y++
        }

        workbook.put("Category", workbook.darkGreyHeaderStyle).y++
        workbook.y++

        for (tokenChain in inputs) {
            if (tokenChain.include) {
                workbook.put(tokenChain.name, workbook.darkGreyHeaderStyle).y++
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

                workbook.put(pstr, workbook.greyHeaderStyle).x++
                workbook.cell().cellStyle = workbook.greyHeaderStyle
                workbook.x--
                workbook.y++

                workbook.put(context, workbook.greyHeaderStyle).x++
                workbook.cell().cellStyle = workbook.greyHeaderStyle
                workbook.x--
                workbook.y++
            }

            workbook.put(result.category.name, workbook.greyHeaderStyle).x++
            workbook.cell().cellStyle = workbook.greyHeaderStyle
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
                workbook.put(str, style).x++
                workbook.put("[" + chunk.value.name + "]", style).y++
                workbook.x--
            }

            workbook.width(40).x++
            workbook.width(15).x++
            workbook.resetY()
        }

        workbook.reset().y += inputs.size + 4
    }

    fun writeSummary(totalDiffs: Int) {
        workbook.width(30).put("Category", workbook.darkGreyHeaderStyle).x++
        workbook.width(20).put("Occurences", workbook.darkGreyHeaderStyle)
        workbook.resetX().y++

        Classifier.root.forEachCategory { category, depth ->
            if (depth == 2) {
                workbook.put(category.name).x++
                workbook.put(category.totalOccurences.toDouble())
                workbook.resetX().y++
            }
        }

        workbook.put(Classifier.unknown.name).x++
        workbook.put(Classifier.unknown.totalOccurences.toDouble())
        workbook.resetX().mark().y++

        workbook.put("total occurences", workbook.greyHeaderStyle).x++
        workbook.put(Classifier.root.totalOccurences.toDouble(), workbook.greyHeaderStyle)
        workbook.resetX().y++
        workbook.put("total diff chunks", workbook.greyHeaderStyle).x++
        workbook.put(totalDiffs.toDouble(), workbook.greyHeaderStyle)
        workbook.resetX().y++

        val chartRect = CellRangeAddress(0, 25, 3, 13)
        val drawing: XSSFDrawing = workbook.sheet!!.createDrawingPatriarch()
        val anchor: XSSFClientAnchor = workbook.anchor(chartRect)

        val chart = drawing.createChart(anchor)
        chart.ctChartSpace.roundedCorners = CTBoolean.Factory.newInstance()
        chart.ctChartSpace.roundedCorners.setVal(false)
        chart.setTitleText("Occurences")
        chart.titleOverlay = false

        val leftAxis = chart.createValueAxis(AxisPosition.LEFT)
        leftAxis.crossBetween = AxisCrossBetween.BETWEEN
        leftAxis.crosses = AxisCrosses.AUTO_ZERO

        val bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM)

        val sourceX = XDDFDataSourcesFactory.fromStringCellRange(workbook.sheet,
            CellRangeAddress(1, workbook.my, 0, 0)
        )

        val sourceY = XDDFDataSourcesFactory.fromNumericCellRange(workbook.sheet,
            CellRangeAddress(1, workbook.my, 1, 1)
        )

        val chartData = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis) as XDDFBarChartData
        chartData.barDirection = BarDirection.BAR

        val series = chartData.addSeries(sourceX, sourceY) as XDDFBarChartData.Series
        series.setTitle("Occurences", null)

        chartData.setVaryColors(true)
        chart.plot(chartData)
    }

}
