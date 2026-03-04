package util;

import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

//文件转换
public class ConvertFile {
    private static Logger log = Logger.getLogger(ConvertFile.class);
    /**
     * 统一转换方法：根据文件扩展名自动选择转换方式
     */
    public static void convertFileToPdf(String inputFilePath, String outputPdfPath) throws Exception {
        File inputFile = new File(inputFilePath);

        if (!inputFile.exists()) {
            throw new FileNotFoundException("文件不存在: " + inputFilePath);
        }

        // 获取文件扩展名
        String fileName = inputFile.getName().toLowerCase();

        if (fileName.endsWith(".txt")) {
            convertTxtToPdf(inputFilePath, outputPdfPath);
        } else if (fileName.endsWith(".docx")) {
            convertDocxToPdf(inputFilePath, outputPdfPath);
        } else if (fileName.endsWith(".doc")) {
            throw new IllegalArgumentException("不支持.doc格式，请先转换为.docx格式");
        } else {
            throw new IllegalArgumentException("不支持的文件格式: " + fileName);
        }
    }

    // ========== TXT文件处理 ==========

    /**
     * 将TXT文件转换为PDF
     */
    public static void convertTxtToPdf(String txtFilePath, String pdfFilePath) throws Exception {
        log.info("开始转换TXT文件: " + txtFilePath);
        // 读取TXT文件内容
        List<String> lines = readTxtFile(txtFilePath);
        log.info("读取到 " + lines.size() + " 行文本");
        // 创建PDF文档
        Document pdfDoc = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(pdfDoc, new FileOutputStream(pdfFilePath));
        pdfDoc.open();

        // 设置中文字体
        BaseFont baseFont = loadChineseFont(pdfDoc);
        Font normalFont = new Font(baseFont, 12, Font.NORMAL);
        Font titleFont = new Font(baseFont, 16, Font.BOLD, BaseColor.BLUE);

        // 添加文件信息
//        addFileHeader(pdfDoc, "TXT文件: " + new File(txtFilePath).getName(), titleFont);

        // 添加分隔线
        pdfDoc.add(new LineSeparator());
        pdfDoc.add(Chunk.NEWLINE);

        // 处理文本行
        int lineCount = 0;
        float yPosition = pdfDoc.top() - 50; // 起始Y坐标

        for (String line : lines) {
            lineCount++;

            // 清理文本
            line = cleanText(line);
            if (line.isEmpty()) {
                // 空行
                pdfDoc.add(Chunk.NEWLINE);
                continue;
            }

            // 检查是否需要分页
            yPosition -= 15; // 每行大约15点
            if (yPosition < 50) {
                pdfDoc.newPage();
                yPosition = pdfDoc.top() - 50;
            }

            // 处理长行自动换行
            List<String> wrappedLines = wrapText(line, baseFont, 12, pdfDoc.right() - pdfDoc.left() - 20);
            for (String wrappedLine : wrappedLines) {
                pdfDoc.add(new Paragraph(wrappedLine, normalFont));
            }
        }

        // 添加页脚
//        addFileFooter(pdfDoc, lineCount, normalFont);

        pdfDoc.close();
        log.info("TXT转换完成: " + pdfFilePath);
    }

    /**
     * 读取TXT文件
     */
    private static List<String> readTxtFile(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
    }

    // ========== DOCX文件处理 ==========

    /**
     * 将DOCX文件转换为PDF
     */
    public static void convertDocxToPdf(String docxFilePath, String pdfFilePath) throws Exception {
        log.info("开始转换DOCX文件: " + docxFilePath);

        try (FileInputStream fis = new FileInputStream(docxFilePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // 创建PDF文档
            Document pdfDoc = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(pdfDoc, new FileOutputStream(pdfFilePath));
            pdfDoc.open();

            // 设置中文字体
            BaseFont baseFont = loadChineseFont(pdfDoc);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font titleFont = new Font(baseFont, 16, Font.BOLD, BaseColor.BLUE);
            Font headingFont = new Font(baseFont, 14, Font.BOLD);

            // 添加文件信息
//            addFileHeader(pdfDoc, "DOCX文件: " + new File(docxFilePath).getName(), titleFont);

            // 添加分隔线
            pdfDoc.add(new LineSeparator());
            pdfDoc.add(Chunk.NEWLINE);

            // 使用Set来避免图片重复
            Set<String> processedPictures = new HashSet<>();
            int paragraphCount = 0;
            int tableCount = 0;
            int imageCount = 0;

            // 1. 处理所有段落
            for (XWPFParagraph para : document.getParagraphs()) {
                paragraphCount++;

                // 获取段落文本
                String text = para.getText();
                if (text != null && !text.trim().isEmpty()) {
                    text = cleanText(text);

                    // 检查段落样式
                    String style = para.getStyle();
                    if (style != null && (style.contains("Heading") || style.contains("Title"))) {
                        // 标题样式
                        pdfDoc.add(new Paragraph(text, headingFont));
                    } else {
                        // 普通段落
                        pdfDoc.add(new Paragraph(text, normalFont));
                    }
                }

                // 处理段落中的图片
                for (XWPFRun run : para.getRuns()) {
                    for (XWPFPicture picture : run.getEmbeddedPictures()) {
                        imageCount++;
                        processDocxPicture(picture, pdfDoc, processedPictures);
                    }
                }
            }

            // 2. 处理所有表格
            for (XWPFTable table : document.getTables()) {
                tableCount++;
                processDocxTable(table, pdfDoc, normalFont, processedPictures);

                // 表格后添加空行
                pdfDoc.add(Chunk.NEWLINE);
            }

            // 3. 处理文档中的所有独立图片
            List<XWPFPictureData> allPictures = document.getAllPictures();
            for (XWPFPictureData pictureData : allPictures) {
                String pictureKey = getPictureKey(pictureData);

                // 只处理未处理过的图片
                if (!processedPictures.contains(pictureKey)) {
                    imageCount++;
                    processDocxPictureData(pictureData, pdfDoc);
                    processedPictures.add(pictureKey);
                }
            }

            // 添加页脚
//            addFileFooter(pdfDoc, paragraphCount, tableCount, imageCount, normalFont);

            pdfDoc.close();

            log.info("DOCX转换完成: " + pdfFilePath);
            log.info("处理统计: " + paragraphCount + " 段落, " + tableCount + " 表格, " + imageCount + " 图片");

        } catch (Exception e) {
            log.error("转换失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理DOCX中的表格
     */
    private static void processDocxTable(XWPFTable table, Document pdfDoc, Font font,
                                         Set<String> processedPictures) throws DocumentException {
        try {
            if (table.getNumberOfRows() == 0) return;

            // 确定列数
            int colCount = 0;
            for (XWPFTableRow row : table.getRows()) {
                colCount = Math.max(colCount, row.getTableCells().size());
            }

            // 创建PDF表格
            PdfPTable pdfTable = new PdfPTable(colCount);
            pdfTable.setWidthPercentage(100);
            pdfTable.setSpacingBefore(10f);
            pdfTable.setSpacingAfter(10f);

            // 处理表格行
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    String cellText = cell.getText();
                    cellText = cleanText(cellText);

                    PdfPCell pdfCell = new PdfPCell(new Phrase(cellText, font));
                    pdfCell.setPadding(5);
                    pdfTable.addCell(pdfCell);
                }
            }

            pdfDoc.add(pdfTable);

        } catch (Exception e) {
            log.error("处理表格失败: " + e.getMessage());
            // 添加错误提示
            pdfDoc.add(new Paragraph("[表格内容无法完全解析]", font));
        }
    }

    /**
     * 处理DOCX图片
     */
    private static void processDocxPicture(XWPFPicture picture, Document pdfDoc,
                                           Set<String> processedPictures) {
        try {
            XWPFPictureData pictureData = picture.getPictureData();
            if (pictureData != null) {
                String pictureKey = getPictureKey(pictureData);

                if (!processedPictures.contains(pictureKey)) {
                    processDocxPictureData(pictureData, pdfDoc);
                    processedPictures.add(pictureKey);
                }
            }
        } catch (Exception e) {
            log.error("处理图片失败: " + e.getMessage());
        }
    }

    /**
     * 处理DOCX图片数据
     */
    private static void processDocxPictureData(XWPFPictureData pictureData, Document pdfDoc) {
        try {
            byte[] imageData = pictureData.getData();
            if (imageData == null || imageData.length == 0) return;

            Image pdfImage = Image.getInstance(imageData);
            pdfImage.scaleToFit(400, 400);
            pdfImage.setAlignment(Element.ALIGN_CENTER);
            pdfDoc.add(pdfImage);

            // 添加图片说明（可选）
            Font captionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
            Paragraph caption = new Paragraph("图片: " + pictureData.getFileName(), captionFont);
            caption.setAlignment(Element.ALIGN_CENTER);
            pdfDoc.add(caption);

            // 图片后添加空行
            pdfDoc.add(Chunk.NEWLINE);

        } catch (Exception e) {
            log.error("创建PDF图片失败: " + e.getMessage());
        }
    }

    // ========== 通用辅助方法 ==========

    /**
     * 加载中文字体
     */
    private static BaseFont loadChineseFont(Document pdfDoc) throws IOException, DocumentException {
        // 尝试多种字体路径
        String[] fontPaths = {
                "C:/Windows/Fonts/simfang.ttf",     // 仿宋（之前成功过）
                "C:/Windows/Fonts/simsun.ttc",      // 宋体
                "C:/Windows/Fonts/msyh.ttc",        // 微软雅黑
                "C:/Windows/Fonts/simhei.ttf"       // 黑体
        };

        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                    log.info("使用字体: " + fontPath);
                    return baseFont;
                } catch (Exception e) {
                    log.error("字体加载失败 " + fontPath + ": " + e.getMessage());
                }
            }
        }

        // 如果所有中文字体都失败，使用英文字体
        log.info("使用默认英文字体，中文可能无法正常显示");
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    /**
     * 清理文本
     */
    private static String cleanText(String text) {
        if (text == null) return "";

        // 移除控制字符
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F\uFFFD]", "");

        // 移除过多的空白
//        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    /**
     * 文本自动换行
     */
    private static List<String> wrapText(String text, BaseFont font, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            // 测试添加这个词后的宽度
            String testLine = currentLine.length() > 0 ?
                    currentLine.toString() + " " + word : word;

            // 估算宽度
            float width = estimateTextWidth(testLine, font, fontSize);

            if (width <= maxWidth) {
                // 可以添加到当前行
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                // 当前行已满
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // 单个词就超过宽度，强制分割
                    lines.add(word);
                    currentLine = new StringBuilder();
                }
            }
        }

        // 添加最后一行
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * 估算文本宽度
     */
    private static float estimateTextWidth(String text, BaseFont font, float fontSize) {
        if (text == null || text.isEmpty()) return 0;

        // 简单估算：每个字符平均宽度
        return text.length() * fontSize * 0.6f;
    }

    /**
     * 获取图片唯一标识
     */
    private static String getPictureKey(XWPFPictureData pictureData) {
        return pictureData.getFileName() + "_" + pictureData.getData().length;
    }

    /**
     * 添加文件头部信息
     */
    private static void addFileHeader(Document pdfDoc, String title, Font titleFont) throws DocumentException {
        // 添加标题
        Paragraph header = new Paragraph(title, titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        pdfDoc.add(header);

        // 添加转换时间
        Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
        Paragraph info = new Paragraph(
                "转换时间: " + new Date() + " | 统一文件转换器",
                infoFont
        );
        info.setAlignment(Element.ALIGN_CENTER);
        pdfDoc.add(info);
    }

    /**
     * 添加TXT文件页脚
     */
    private static void addFileFooter(Document pdfDoc, int lineCount, Font font) throws DocumentException {
        pdfDoc.add(Chunk.NEWLINE);
        pdfDoc.add(new LineSeparator());

        Paragraph footer = new Paragraph(
                "总计 " + lineCount + " 行文本 | TXT转PDF转换完成",
                font
        );
        footer.setAlignment(Element.ALIGN_RIGHT);
        pdfDoc.add(footer);
    }

    /**
     * 添加DOCX文件页脚
     */
    private static void addFileFooter(Document pdfDoc, int paragraphCount, int tableCount,
                                      int imageCount, Font font) throws DocumentException {
        pdfDoc.add(Chunk.NEWLINE);
        pdfDoc.add(new LineSeparator());

        String stats = String.format(
                "统计: %d 段落, %d 表格, %d 图片 | DOCX转PDF转换完成",
                paragraphCount, tableCount, imageCount
        );

        Paragraph footer = new Paragraph(stats, font);
        footer.setAlignment(Element.ALIGN_RIGHT);
        pdfDoc.add(footer);
    }

    // ========== 批量转换功能 ==========

    /**
     * 批量转换多个文件
     */
    public static List<String> batchConvertFiles(List<String> inputFiles) throws Exception {
        log.info("=== 开始批量转换 ===");
        List<String> outputFiles = new ArrayList<>();
//        File outputDirectory = new File(outputDir);
//        if (!outputDirectory.exists()) {
//            outputDirectory.mkdirs();
//        }

        int successCount = 0;
        int failCount = 0;

        for (String inputFile : inputFiles) {
            try {
                File file = new File(inputFile);
                if (!file.exists()) {

                    log.error("文件不存在: " + inputFile);
                    failCount++;
                    continue;
                }

                // 生成输出文件名
                String baseName = inputFile.substring(0, inputFile.lastIndexOf('.'));
                log.info("baseName:"+baseName);
                String outputFile =  baseName + ".pdf";

                log.info("转换: " + inputFile + " -> " + outputFile);

                // 转换文件
                convertFileToPdf(inputFile, outputFile);
                successCount++;
                outputFiles.add(outputFile);
            } catch (Exception e) {
                log.error("转换失败 " + inputFile + ": " + e.getMessage());
                failCount++;
            }
        }

        log.info("\n批量转换完成:");
        log.info("  成功: " + successCount + " 个文件");
        log.info("  失败: " + failCount + " 个文件");
        return outputFiles;
    }

//    excel转PDF
private static final Rectangle PDF_PAGE_SIZE = PageSize.A4;
    private static final float MARGIN = 20;
    private static final float CELL_HEIGHT = 25;          // 固定行高（点）
    private static final int FONT_SIZE = 10;
    private static final int MAX_ROWS_PER_PAGE = 25;       // 每页最大行数
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\x00-\\x1F\\x7F]");
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(); // 格式化单元格值

    public static void convertExcelToPdf(String excelPath, String pdfPath) throws Exception {
        if (excelPath == null || !excelPath.matches("^.+\\.(xlsx|xls)$")) {
            throw new IllegalArgumentException("Excel路径无效，仅支持.xlsx/.xls格式");
        }
        if (pdfPath == null || !pdfPath.endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF路径无效，必须以.pdf结尾");
        }

        Workbook workbook = null;
        Document document = null;
        PdfWriter writer = null;
        try {
            workbook = WorkbookFactory.create(new FileInputStream(excelPath));
            document = new Document(PDF_PAGE_SIZE, MARGIN, MARGIN, MARGIN, MARGIN);
            writer = PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            boolean anySheetProcessed = false;
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (sheet == null || sheet.getLastRowNum() < 0) {
                    log.info("跳过空工作表：" + sheet.getSheetName());
                    continue;
                }
                drawSheetToPdf(document, sheet, writer);
                anySheetProcessed = true;
            }

            if (!anySheetProcessed) {
                throw new IllegalArgumentException("Excel文件中没有包含数据的工作表，无法生成PDF。");
            }

            log.info("Excel转PDF成功！输出路径：" + pdfPath);
        } catch (Exception e) {
            log.error("Excel转PDF失败：" + e.getMessage());
            throw e;
        } finally {
            if (document != null) document.close();
            if (writer != null) writer.close();
            if (workbook != null) workbook.close();
        }
    }

    // ==================== 绘制单个工作表 ====================
    private static void drawSheetToPdf(Document document, Sheet sheet, PdfWriter writer) throws Exception {
        Font font = getChineseFont();
        int maxColNum = getMaxColumnNum(sheet);
        log.info("工作表：" + sheet.getSheetName() + "，最大列数：" + maxColNum);

        // 计算表格总宽度（占文档可用宽度的80%）
        float pageWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
        float totalWidth = pageWidth * 0.8f;

        // 初始化表格
        PdfPTable table = new PdfPTable(maxColNum);
        setTableWidths(table, maxColNum, totalWidth);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        int rowCount = 0;
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                addEmptyRow(table, maxColNum);
                rowCount++;
                continue;
            }

            for (int colIndex = 0; colIndex < maxColNum; colIndex++) {
                Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String cellValue = DATA_FORMATTER.formatCellValue(cell); // 使用 DataFormatter 保留格式
                cellValue = filterControlCharacters(cellValue);

                PdfPCell cellPdf = new PdfPCell(new Paragraph(cellValue, font));
                cellPdf.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellPdf.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellPdf.setBorder(Rectangle.BOX);
                cellPdf.setFixedHeight(CELL_HEIGHT);
                table.addCell(cellPdf);
            }

            rowCount++;
            if (rowCount >= MAX_ROWS_PER_PAGE) {
                // 绘制当前页表格及其图片
                drawTableAndPictures(document, sheet, table, writer, rowIndex - rowCount + 1, rowIndex);
                // 重置表格
                table = new PdfPTable(maxColNum);
                setTableWidths(table, maxColNum, totalWidth);
                table.setSpacingBefore(10);
                table.setSpacingAfter(10);
                rowCount = 0;
            }
        }

        // 绘制剩余行
        if (table.getRows().size() > 0) {
            drawTableAndPictures(document, sheet, table, writer,
                    sheet.getLastRowNum() - rowCount + 1, sheet.getLastRowNum());
        }

        // 如果需要在此工作表的表格下方添加额外文本，可在此处添加
        // document.add(new Paragraph("这里是表格下方的额外内容", font));

        document.newPage();
    }

    // ==================== 设置表格宽度（等宽）====================
    private static void setTableWidths(PdfPTable table, int columnCount, float totalWidth) throws DocumentException {
        float[] colWidths = new float[columnCount];
        Arrays.fill(colWidths, totalWidth / columnCount);
        table.setTotalWidth(colWidths);
        table.setLockedWidth(true);
    }

    // ==================== 绘制表格并添加图片 ====================
    private static void drawTableAndPictures(Document document, Sheet sheet,
                                             PdfPTable table, PdfWriter writer,
                                             int startRow, int endRow) throws Exception {
        if (table.getTotalWidth() <= 0) {
            log.error("警告：表格宽度为0，跳过绘制");
            return;
        }

        PdfContentByte canvas = writer.getDirectContent();
        // 计算起始X坐标（居中）
        float pageWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
        float startX = document.leftMargin() + (pageWidth - table.getTotalWidth()) / 2;
        float pageHeight = document.getPageSize().getTop() - document.topMargin();
        float yPos = pageHeight - table.getTotalHeight();

        // 绘制表格
        table.writeSelectedRows(0, -1, startX, yPos, canvas);

        // 获取图片对象
        Drawing<?> drawing = sheet.getDrawingPatriarch();
        if (drawing == null) return;

        List<?> shapes = null;
        if (drawing instanceof XSSFDrawing) {
            shapes = ((XSSFDrawing) drawing).getShapes();
        } else if (drawing instanceof HSSFPatriarch) {
            shapes = ((HSSFPatriarch) drawing).getChildren();
        }
        if (shapes == null) return;

        for (Object shapeObj : shapes) {
            if (!(shapeObj instanceof Picture)) continue;
            Picture pic = (Picture) shapeObj;
            ClientAnchor anchor = (ClientAnchor) pic.getAnchor();
            int picRow1 = anchor.getRow1();
            int picRow2 = anchor.getRow2();

            // 判断图片是否属于当前页（行范围）
            if ((picRow1 < startRow || picRow1 > endRow) &&
                    (picRow2 < startRow || picRow2 > endRow)) {
                continue;
            }

            byte[] picData = pic.getPictureData().getData();
            Image img = Image.getInstance(picData);

            // 计算图片所在列范围
            int col1 = anchor.getCol1();
            int col2 = anchor.getCol2();
            float[] colWidths = table.getAbsoluteWidths();
            float x = startX;
            for (int i = 0; i < col1; i++) {
                x += colWidths[i];
            }
            float picWidth = 0;
            for (int i = col1; i <= col2; i++) {
                picWidth += colWidths[i];
            }

            // 计算图片行位置
            int relativeRow = picRow1 - startRow;
            float yFromTableTop = (relativeRow + 1) * CELL_HEIGHT; // 单元格底部到表格顶部的距离
            float y = yPos - yFromTableTop + CELL_HEIGHT;          // 单元格顶部Y坐标

            int rowSpan = picRow2 - picRow1 + 1;
            float picHeight = rowSpan * CELL_HEIGHT;

            img.scaleToFit(picWidth, picHeight);
            img.setAbsolutePosition(x, y - img.getScaledHeight());
            canvas.addImage(img);
        }
    }

    // ==================== 辅助方法 ====================

    /** 获取工作表最大列数 */
    private static int getMaxColumnNum(Sheet sheet) {
        int maxCol = 0;
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && row.getLastCellNum() > maxCol) {
                maxCol = row.getLastCellNum();
            }
        }
        return maxCol == 0 ? 1 : maxCol;
    }

    /** 添加空行（占位） */
    private static void addEmptyRow(PdfPTable table, int colNum) {
        Font font = getChineseFont();
        for (int i = 0; i < colNum; i++) {
            PdfPCell cell = new PdfPCell(new Paragraph("", font));
            cell.setBorder(Rectangle.BOX);
            cell.setFixedHeight(CELL_HEIGHT);
            table.addCell(cell);
        }
    }

    /** 获取中文字体（iTextAsian 或系统字体） */
    private static Font getChineseFont() {
        try {
            // 使用 iTextAsian 字体包
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(baseFont, FONT_SIZE, Font.NORMAL);
        } catch (Exception e) {
            // 如果 iTextAsian 不可用，尝试使用系统字体（Windows 示例）
            try {
                BaseFont baseFont = BaseFont.createFont("C:/Windows/Fonts/simsun.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                return new Font(baseFont, FONT_SIZE, Font.NORMAL);
            } catch (Exception ex) {
                // 最终使用 Helvetica 作为后备字体（中文会显示为方框）
                return FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE, Font.NORMAL);
            }
        }
    }

    /** 过滤不可打印控制字符 */
    private static String filterControlCharacters(String text) {
        if (text == null) return "";
        return CONTROL_CHAR_PATTERN.matcher(text).replaceAll("");
    }

}
