package dev.m00nl1ght.tokdiff.export

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xddf.usermodel.XDDFLineProperties
import org.apache.poi.xssf.usermodel.*


class WorkbookRefs constructor(val root: XSSFWorkbook) {

    enum class Style {
        Normal, Subheader, Header
    }

    val headerFont: XSSFFont
    val colorStyles: List<CellStyle>
    val baseStyles: Array<CellStyle>
    val numericStyles: Array<CellStyle>
    val numericFormat: DataFormat
    val tinyLine: XDDFLineProperties

    var sheet: XSSFSheet? = null
    var x = 0
    var y = 0
    var mx = 0
    var my = 0

    val cellref: CellReference
        get() = CellReference(y, x)

    init {
        headerFont = root.createFont()
        headerFont.bold = true

        numericFormat = root.createDataFormat()

        colorStyles = sequenceOf(
            byteArrayOf(238.toByte(), 238.toByte(), 238.toByte()),
            byteArrayOf(144.toByte(), 202.toByte(), 249.toByte()),
            byteArrayOf(165.toByte(), 214.toByte(), 167.toByte()),
            byteArrayOf(230.toByte(), 238.toByte(), 156.toByte()),
            byteArrayOf(206.toByte(), 147.toByte(), 216.toByte()),
            byteArrayOf(239.toByte(), 154.toByte(), 154.toByte()),
            byteArrayOf(255.toByte(), 204.toByte(), 128.toByte()),
        ).map { c ->
            val style: XSSFCellStyle = root.createCellStyle()
            style.setFillForegroundColor(XSSFColor(c))
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
            style
        }.toList()

        baseStyles = Style.values().map(::baseStyle).toTypedArray()
        numericStyles = Style.values().map(::numericStyle).toTypedArray()

        tinyLine = XDDFLineProperties()
        tinyLine.width = 0.0
    }

    private fun baseStyle(kind: Style): XSSFCellStyle {
        val style = root.createCellStyle()
        when (kind) {
            Style.Subheader -> {
                val rgb = byteArrayOf(220.toByte(), 220.toByte(), 220.toByte())
                style.fillPattern = FillPatternType.SOLID_FOREGROUND
                style.setFillForegroundColor(XSSFColor(rgb))
            }
            Style.Header -> {
                val rgb = byteArrayOf(160.toByte(), 160.toByte(), 160.toByte())
                style.fillPattern = FillPatternType.SOLID_FOREGROUND
                style.setFillForegroundColor(XSSFColor(rgb))
                style.setFont(headerFont)
            }
            else -> {}
        }
        return style
    }

    private fun numericStyle(kind: Style, textColor: XSSFColor? = null): XSSFCellStyle {
        val style = baseStyle(kind)
        style.dataFormat = numericFormat.getFormat("0.00")
        if (textColor != null) {
            val font = root.createFont()
            if (kind == Style.Header) font.bold = true
            font.setColor(textColor)
            style.setFont(font)
        }
        return style
    }

    fun row(): Row = row(sheet!!, y)

    fun row(style: Style) {
        row().rowStyle = baseStyles[style.ordinal]
    }

    fun cell(): Cell = cell(sheet!!, y, x)

    fun cell(style: Style) {
        cell().cellStyle = baseStyles[style.ordinal]
    }

    fun put(str: String, style: Style = Style.Normal): WorkbookRefs {
        val cell = cell()
        cell.setCellValue(str)
        if (style != Style.Normal) cell.cellStyle = baseStyles[style.ordinal]
        return this
    }

    fun put(value: Double, style: Style = Style.Normal, decimalFormat: Boolean = true): WorkbookRefs {
        val cell = cell()
        cell.setCellValue(value)
        cell.cellStyle = if(decimalFormat) numericStyles[style.ordinal] else baseStyles[style.ordinal]
        return this
    }

    fun putFormula(formula: String, style: Style = Style.Normal): WorkbookRefs {
        val cell = cell()
        cell.cellFormula = formula
        cell.cellStyle = numericStyles[style.ordinal]
        return this
    }

    fun width(width: Int): WorkbookRefs {
        sheet?.setColumnWidth(x, width * 256)
        return this
    }

    fun mark(): WorkbookRefs {
        mx = x
        my = y
        return this
    }

    fun reset(): WorkbookRefs {
        x = mx
        y = my
        return this
    }

    fun resetX(): WorkbookRefs {
        x = mx
        return this
    }

    fun resetY(): WorkbookRefs {
        y = my
        return this
    }

    fun clearMark(): WorkbookRefs {
        mx = 0
        my = 0
        return this
    }

    fun row(sheet: Sheet, rownum: Int): Row {
        val row = sheet.getRow(rownum)
        return row ?: sheet.createRow(rownum)
    }

    fun cell(row: Row, colnum: Int): Cell {
        val cell = row.getCell(colnum)
        return cell ?: row.createCell(colnum)
    }

    fun cell(sheet: Sheet, rownum: Int, colnum: Int): Cell {
        val row = row(sheet, rownum)
        val cell = row.getCell(colnum)
        return cell ?: row.createCell(colnum)
    }

    fun cellExists(sheet: Sheet, rownum: Int, colnum: Int): Boolean {
        val row = sheet.getRow(rownum) ?: return false
        val cell = row.getCell(colnum)
        return cell != null
    }

    fun colorStyleFor(idx: Int): CellStyle? {
        return if (idx < 0 || idx >= colorStyles.size) null else colorStyles[idx]
    }

    fun anchor(address: CellRangeAddress): XSSFClientAnchor {
        val drawing = sheet!!.createDrawingPatriarch()
        return drawing.createAnchor(0, 0, 0, 0,
            address.firstColumn, address.firstRow, address.lastColumn, address.lastRow)
    }

    fun applyConditionalFormatting(range: CellRangeAddress, posNeg: Boolean = true, zero: Boolean = true) {
        val green = XSSFColor(byteArrayOf(0.toByte(), 120.toByte(), 0.toByte()))
        val grey = XSSFColor(byteArrayOf(160.toByte(), 160.toByte(), 160.toByte()))
        val red = XSSFColor(byteArrayOf(220.toByte(), 0.toByte(), 0.toByte()))

        if (posNeg) {
            val ruleGreen = sheet!!.sheetConditionalFormatting.createConditionalFormattingRule(
                ComparisonOperator.GT, "0.0", null
            )

            val ruleRed = sheet!!.sheetConditionalFormatting.createConditionalFormattingRule(
                ComparisonOperator.LT, "0.0", null
            )

            ruleGreen.createFontFormatting().fontColor = green
            ruleRed.createFontFormatting().fontColor = red

            sheet!!.sheetConditionalFormatting.addConditionalFormatting(arrayOf(range), ruleGreen)
            sheet!!.sheetConditionalFormatting.addConditionalFormatting(arrayOf(range), ruleRed)
        }

        if (zero) {
            val ruleGrey = sheet!!.sheetConditionalFormatting.createConditionalFormattingRule(
                ComparisonOperator.EQUAL, "0.0", null
            )

            ruleGrey.createFontFormatting().fontColor = grey

            sheet!!.sheetConditionalFormatting.addConditionalFormatting(arrayOf(range), ruleGrey)
        }
    }
}
