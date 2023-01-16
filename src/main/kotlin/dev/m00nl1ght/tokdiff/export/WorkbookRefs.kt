package dev.m00nl1ght.tokdiff.export

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xddf.usermodel.XDDFLineProperties
import org.apache.poi.xssf.usermodel.*

class WorkbookRefs constructor(val root: XSSFWorkbook) {

    val headerFont: XSSFFont
    val colorStyles: List<CellStyle>
    val greyHeaderStyle: CellStyle
    val darkGreyHeaderStyle: CellStyle
    val numericFormat: DataFormat
    val numericStyle: CellStyle
    val tinyLine: XDDFLineProperties

    var sheet: XSSFSheet? = null
    var x = 0
    var y = 0
    var mx = 0
    var my = 0

    init {
        headerFont = root.createFont()
        headerFont.bold = true

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

        greyHeaderStyle = root.createCellStyle()
        greyHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex())
        greyHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

        darkGreyHeaderStyle = root.createCellStyle()
        darkGreyHeaderStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex())
        darkGreyHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        darkGreyHeaderStyle.setFont(headerFont)

        numericFormat = root.createDataFormat()
        numericStyle = root.createCellStyle()
        numericStyle.setDataFormat(numericFormat.getFormat("0.00"))

        tinyLine = XDDFLineProperties()
        tinyLine.width = 0.0
    }

    fun row(): Row = row(sheet!!, y)

    fun cell(): Cell = cell(sheet!!, y, x)

    fun put(str: String, style: CellStyle? = null): WorkbookRefs {
        val cell = cell()
        cell.setCellValue(str)
        if (style != null) cell.cellStyle = style
        return this
    }

    fun put(value: Double, style: CellStyle? = null): WorkbookRefs {
        val cell = cell()
        cell.setCellValue(value)
        if (style != null) cell.cellStyle = style
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
}
