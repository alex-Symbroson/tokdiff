package dev.m00nl1ght.tokdiff

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xddf.usermodel.XDDFLineProperties
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.util.stream.Stream

class WorkbookRefs constructor(val root: XSSFWorkbook) {

    val headerFont: XSSFFont
    val colorStyles: Array<CellStyle>
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

        colorStyles = Stream.of(
            IndexedColors.GREEN,
            IndexedColors.LIGHT_BLUE,
            IndexedColors.LIGHT_ORANGE,
            IndexedColors.PLUM
        ).map { c ->
            val style: CellStyle = root.createCellStyle()
            style.fillForegroundColor = c.getIndex()
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
            style
        }.toArray{size -> arrayOfNulls<CellStyle>(size)}

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

    fun colorStyleFor(idx: Int): CellStyle {
        return if (idx < 0 || idx > colorStyles.size - 2) colorStyles[colorStyles.size - 1] else colorStyles[idx]
    }

    fun anchor(sheet: XSSFSheet, address: CellRangeAddress): XSSFClientAnchor {
        val drawing = sheet.createDrawingPatriarch()
        return drawing.createAnchor(0, 0, 0, 0,
            address.firstColumn, address.firstRow, address.lastColumn, address.lastRow)
    }
}
