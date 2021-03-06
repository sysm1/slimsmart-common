package cn.slimsmart.common.document.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import cn.slimsmart.common.document.excel.annotation.Excel;
import cn.slimsmart.common.document.excel.annotation.ExcelAnnotationTool;
import cn.slimsmart.common.document.excel.annotation.support.CellMeta;
import cn.slimsmart.common.document.excel.annotation.support.Types;
import cn.slimsmart.common.document.excel.style.Styles;
import cn.slimsmart.common.document.excel.support.ExcelType;
import cn.slimsmart.common.util.common.date.DateUtil;
import cn.slimsmart.common.util.common.string.StringUtil;
import cn.slimsmart.common.util.document.excel.ExcelUtil;
import cn.slimsmart.common.util.reflect.ReflectionUtil;

//http://blog.sina.com.cn/s/blog_49f779790101ck67.html
//http://www.doc88.com/p-206930999655.html
//http://blog.sina.com.cn/s/blog_9c760553010172v5.html
//http://www.oschina.net/question/166633_82410
@SuppressWarnings({ "deprecation", "rawtypes" })
public class ExportExcel<T> {

	private Logger log = Logger.getLogger(ExportExcel.class.getName());

	private Class<T> entityClass;

	public ExportExcel(Class<T> clazz) {
		this.entityClass = clazz;
	}

	private void renderHeaders(final Workbook workbook, final Sheet sheet, final Map<String, Object> cellMeats, int level, final int headLevel) {
		// 产生表格标题行
		Row row = sheet.getRow(level) == null ? sheet.createRow(level) : sheet.getRow(level);
		Cell cell = null;
		for (Entry entry : cellMeats.entrySet()) {
			CellMeta cellMeat = (CellMeta) entry.getValue();
			switch (cellMeat.getCell().type()) {
			case Types.BOOLEAN:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_BOOLEAN);
				break;
			case Types.INTEGER:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				break;
			case Types.LONG:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				break;
			case Types.FLOAT:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				break;
			case Types.DOUBLE:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				break;
			case Types.STRING:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_STRING);
				break;
			case Types.DATE:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_STRING);
				break;
			case Types.POJO:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_STRING);
				break;
			default:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()));
				break;
			}

			RichTextString text = workbook.getCreationHelper().createRichTextString(cellMeat.getDisplay());
			cell.setCellValue(text);
			log.info("生成Excel头：标题[" + level + "," + Integer.valueOf(entry.getKey().toString()) + "]-->" + text);
			if (level < headLevel - 1 && cellMeat.getCell().type() != Types.POJO) {
				sheet.addMergedRegion(new CellRangeAddress(level, headLevel - 1, Integer.valueOf(entry.getKey().toString()), Integer.valueOf(entry.getKey()
						.toString())));
				log.info("生成Excel头：合并单元格[" + level + "," + Integer.valueOf(entry.getKey().toString()) + "]-->[" + (headLevel - 1) + ","
						+ Integer.valueOf(entry.getKey().toString()) + "]");
			}
			if (cellMeat.getCell().type() == Types.POJO) {
				sheet.addMergedRegion(new CellRangeAddress(level, level, Integer.valueOf(entry.getKey().toString()), Integer.valueOf(entry.getKey().toString())
						+ cellMeat.getCell().length() - 1));
				log.info("生成Excel头：合并单元格[" + level + "," + Integer.valueOf(entry.getKey().toString()) + "]-->[" + level + ","
						+ (Integer.valueOf(entry.getKey().toString()) + cellMeat.getCell().length() - 1) + "]");
				renderHeaders(workbook, sheet,
						ExcelAnnotationTool.getFieldCellMeats(cellMeat.getField().getType(), Integer.valueOf(entry.getKey().toString()), false), level + 1,
						headLevel);
			}
		}
	}

	public void exportExcelTemplate(OutputStream out, int excelType) {
		Excel excelAnnotation = ExcelAnnotationTool.getTypeExcelAnnotation(entityClass);
		// 声明一个工作薄
		Workbook workbook = ExcelUtil.getWorkbook(excelType);
		// 生成一个表格
		Sheet sheet = workbook.createSheet(excelAnnotation.title());
		Map<String, Object> cellMeats = ExcelAnnotationTool.getFieldCellMeats(entityClass, 0, true);
		int rowLen = ExcelAnnotationTool.getRowCount(entityClass);
		int colLen = (Integer) cellMeats.get(ExcelAnnotationTool.COLUMN_LENGTH);
		cellMeats.remove(ExcelAnnotationTool.COLUMN_LENGTH);
		renderHeaders(workbook, sheet, cellMeats, 0, rowLen);
		Row row = null;
		for (int i = 0; i < rowLen; i++) {
			row = sheet.getRow(i);
			if (row == null)
				row = sheet.createRow(i);
			for (int j = 0; j < colLen; j++) {
				Cell cell = row.getCell(j);
				if (cell == null) {
					cell = row.createCell(j);
					cell.setCellValue("");
				}
				cell.setCellStyle(Styles.getTitleStyle(workbook));
				sheet.setColumnWidth(j, excelAnnotation.columnWidth() * 256);
			}
			row.setHeight((short) (excelAnnotation.rowHeight() * 20));
		}
		sheet.createFreezePane(colLen, rowLen);
		// 遍历列模板数据
		renderTmplData(sheet,rowLen,excelType,cellMeats);
		try {
			workbook.write(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void renderTmplData(final Sheet sheet,final int rowLen,final int excelType, final Map<String, Object> cellMeats){
		for (Entry entry : cellMeats.entrySet()) {
			CellMeta cellMeat = (CellMeta) entry.getValue();
			 if(cellMeat.getCell().type() == Types.POJO){
				 renderTmplData(sheet,rowLen,excelType,
						 ExcelAnnotationTool.getFieldCellMeats(cellMeat.getField().getType(), Integer.valueOf(entry.getKey().toString()), false));
			 }else{
				 if(StringUtil.isNotBlank(cellMeat.getCell().selectTmplData())){
					 //Excel 最大行号  2003：65536   2007：1048576
					 ExcelUtil.setValidation(sheet, cellMeat.getCell().selectTmplData().split(";"), rowLen, excelType==ExcelType.EXCEL_2003 ? 
							 65535 : 1048575, Integer.valueOf(entry.getKey().toString()), Integer.valueOf(entry.getKey().toString()));
				 }else{
					 if(StringUtil.isNotBlank(cellMeat.getCell().cellTipMsg())){
						 ExcelUtil.setPrompt(sheet, "提示信息",cellMeat.getCell().cellTipMsg(), rowLen, excelType==ExcelType.EXCEL_2003 ? 
								 65535 : 1048575, Integer.valueOf(entry.getKey().toString()), Integer.valueOf(entry.getKey().toString()));
					 }
				 }
			 }
		}
	}

	public void exportExcel(Collection<T> dataset, OutputStream out, int excelType) {
		Excel excelAnnotation = ExcelAnnotationTool.getTypeExcelAnnotation(entityClass);
		// 声明一个工作薄
		Workbook workbook = ExcelUtil.getWorkbook(excelType);
		// 生成一个表格
		Sheet sheet = workbook.createSheet(excelAnnotation.title());
		Map<String, Object> cellMeats = ExcelAnnotationTool.getFieldCellMeats(entityClass, 0, true);
		int rowLen = ExcelAnnotationTool.getRowCount(entityClass);
		int colLen = (Integer) cellMeats.get(ExcelAnnotationTool.COLUMN_LENGTH);
		cellMeats.remove(ExcelAnnotationTool.COLUMN_LENGTH);
		renderHeaders(workbook, sheet, cellMeats, 0, rowLen);
		Row row = null;
		for (int i = 0; i < rowLen; i++) {
			row = sheet.getRow(i);
			if (row == null)
				row = sheet.createRow(i);
			for (int j = 0; j < colLen; j++) {
				Cell cell = row.getCell(j);
				if (cell == null) {
					cell = row.createCell(j);
					cell.setCellValue("");
				}
				cell.setCellStyle(Styles.getTitleStyle(workbook));
				sheet.setColumnWidth(j, excelAnnotation.columnWidth() * 256);
			}
			row.setHeight((short) (excelAnnotation.rowHeight() * 20));
		}
		sheet.createFreezePane(colLen, rowLen);
		// 遍历集合数据，产生数据行
		Iterator<T> it = dataset.iterator();
		int index = rowLen;
		while (it.hasNext()) {
			row = sheet.createRow(index);
			T t = (T) it.next();
			fillRowData(workbook, sheet, row, t, cellMeats);
			row.setHeight((short) (excelAnnotation.rowHeight() * 20));
			index++;
		}
		try {
			workbook.write(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void fillRowData(final Workbook workbook, final Sheet sheet, final Row row, Object obj, Map<String, Object> cellMeats) {
		Cell cell = null;
		for (Entry entry : cellMeats.entrySet()) {
			CellMeta cellMeat = (CellMeta) entry.getValue();
			sheet.setColumnWidth(Integer.valueOf(entry.getKey().toString()), cellMeat.getCell().width() * 256);
			Object value = null;
			RichTextString text = null;
			CellStyle cellStyle = Styles.getCellStyle(workbook);
			switch (cellMeat.getCell().type()) {
			// 数据验证待添加
			case Types.BOOLEAN:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_BOOLEAN);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				cell.setCellValue((Boolean) value);
				cell.setCellStyle(cellStyle);
				break;
			case Types.INTEGER:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				cell.setCellValue(Double.valueOf(value.toString()));
				cell.setCellStyle(cellStyle);
				break;
			case Types.LONG:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				cell.setCellValue(Double.valueOf(value.toString()));
				cell.setCellStyle(cellStyle);
				break;
			case Types.FLOAT:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				if (StringUtil.isNotBlank(cellMeat.getCell().dataFormat())) {
					DecimalFormat df = new DecimalFormat(cellMeat.getCell().dataFormat());
					cell.setCellValue(df.format(value));
				} else {
					cell.setCellValue(Float.valueOf(value.toString()));
				}
				cell.setCellStyle(cellStyle);
				break;
			case Types.DOUBLE:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_NUMERIC);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				if (StringUtil.isNotBlank(cellMeat.getCell().dataFormat())) {
					DecimalFormat df = new DecimalFormat(cellMeat.getCell().dataFormat());
					cell.setCellValue(df.format(value));
				} else {
					cell.setCellValue(Double.valueOf(value.toString()));
				}
				cell.setCellStyle(cellStyle);
				break;
			case Types.STRING:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_STRING);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				if (StringUtil.isNotBlank(cellMeat.getCell().dataFormat())) {
					value = StringUtil.format(cellMeat.getCell().dataFormat(), value.toString());
				}
				text = workbook.getCreationHelper().createRichTextString(value.toString());
				cell.setCellValue(text);
				cell.setCellStyle(cellStyle);
				break;
			case Types.DATE:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_STRING);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				String dateFormat = StringUtil.isBlank(cellMeat.getCell().dataFormat()) ? DateUtil.YYYY_MM_DD_HH_MM_SS : cellMeat.getCell().dataFormat();
				cell.setCellValue((Date) value);
				cell.setCellStyle(Styles.getCellDateStyle(workbook, dateFormat));
				break;
			case Types.LINK:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()), Cell.CELL_TYPE_STRING);
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				text = workbook.getCreationHelper().createRichTextString(String.valueOf(value));
				cell.setCellValue(text);
				cell.setCellStyle(cellStyle);
				cell.setHyperlink(Styles.getCellLinkStyle(workbook, String.valueOf(value)));
				break;
			case Types.POJO:
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				fillRowData(workbook, sheet, row, value,
						ExcelAnnotationTool.getFieldCellMeats(value.getClass(), Integer.valueOf(entry.getKey().toString()), false));
				break;
			default:
				cell = row.createCell(Integer.valueOf(entry.getKey().toString()));
				value = ReflectionUtil.getFieldValue(obj, cellMeat.getField());
				text = workbook.getCreationHelper().createRichTextString(String.valueOf(value));
				cell.setCellValue(text);
				cell.setCellStyle(cellStyle);
				break;
			}
			if (cell != null) {
				log.info("单元格填充数据：[" + row.getRowNum() + "," + cell.getColumnIndex() + "]-->" + value);
			}
		}
	}
}
