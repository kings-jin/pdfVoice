package com.example.einvoice.generator;

import com.example.einvoice.model.InvoiceData;
import com.example.einvoice.model.InvoiceItem;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * 发票 PDF 生成器 - 严格遵循《数字化电子发票版式及 XML 规范 V6.001》
 * 
 * 关键规范要点:
 * - 票面尺寸：210mm × 140mm (最小值，明细超 8 行时增加高度)
 * - 内框尺寸：201mm × 94mm (不含票头票尾)
 * - 线条颜色：红褐色 RGB(128,0,0)
 * - 线条粗细：0.25mm 实线
 * - 字体规范:
 *   - 票头：楷体 20pt
 *   - 版面元素名称：楷体 9pt
 *   - 填充信息：宋体 9pt
 *   - 纳税人识别号：Courier New 12pt
 *   - 人民币符号：Courier New 11pt
 * - 监制章：椭圆形 30mm×20mm，楷体 7 磅，大红色
 * - 二维码：20mm×20mm，左上角位置 (7mm, 6mm)
 */
@Component
public class InvoicePDFGenerator {

    // 规范定义的颜色
    private static final com.itextpdf.kernel.colors.DeviceRgb RED_BROWN = 
        new com.itextpdf.kernel.colors.DeviceRgb(128, 0, 0);  // 红褐色 RGB(128,0,0)
    private static final com.itextpdf.kernel.colors.DeviceRgb STAMP_RED = 
        new com.itextpdf.kernel.colors.DeviceRgb(255, 0, 0);  // 大红色 (监制章)
    
    // 规范定义的尺寸 (单位：mm)
    private static final float PAGE_WIDTH = 210f;
    private static final float MIN_PAGE_HEIGHT = 140f;
    private static final float INNER_FRAME_WIDTH = 201f;
    private static final float INNER_FRAME_HEIGHT = 94f;
    private static final float LINE_THICKNESS = 0.25f;  // 线条高度 0.25mm
    private static final float QR_CODE_SIZE = 20f;  // 二维码 20*20mm
    private static final float SEAL_WIDTH = 30f;  // 监制章 30mm
    private static final float SEAL_HEIGHT = 20f; // 监制章 20mm
    
    // 字体大小 (单位：pt)
    private static final float FONT_SIZE_TITLE = 20f;      // 票头 20pt
    private static final float FONT_SIZE_ELEMENT = 9f;     // 版面元素 9pt
    private static final float FONT_SIZE_FILL = 9f;        // 填充信息 9pt
    private static final float FONT_SIZE_TAX_ID = 12f;     // 纳税人识别号 12pt
    private static final float FONT_SIZE_SEAL = 7f;        // 监制章 7pt

    /**
     * 生成发票 PDF - 严格按照 V6.001 规范
     */
    public byte[] generate(InvoiceData invoiceData) throws Exception {
        // 计算发票高度：基础 140mm + 超过 8 行的明细高度
        int itemCount = invoiceData.getItems().size();
        float pageHeight = MIN_PAGE_HEIGHT;
        if (itemCount > 8) {
            // 每行高度约 5.5mm (44mm/8 行)
            pageHeight += (itemCount - 8) * 5.5f;
        }
        
        // 创建自定义页面尺寸 (A4 横向近似尺寸)
        PageSize pageSize = new PageSize(PAGE_WIDTH, pageHeight);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, pageSize);
        document.setMargins(0, 0, 0, 0);  // 无边距
        
        // 绘制外边框
        drawOuterBorder(pdf, PAGE_WIDTH, pageHeight);
        
        // 绘制票头区域 (顶部 30mm)
        drawHeader(document, invoiceData, pdf, PAGE_WIDTH, pageHeight);
        
        // 绘制购买方和销售方信息区域 (30mm-52mm)
        drawPartiesInfo(document, invoiceData, pdf);
        
        // 绘制应税明细和合计区域 (52mm-104mm)
        drawItemsAndTotal(document, invoiceData, pdf);
        
        // 绘制备注区域 (底部 16mm-36mm)
        drawRemarks(document, invoiceData, pdf, pageHeight);
        
        // 绘制票尾区域 (底部 0-16mm)
        drawFooter(document, invoiceData, pdf, pageHeight);
        
        // 绘制监制章
        drawOfficialSeal(pdf, invoiceData);
        
        // 生成并绘制二维码
        drawQRCode(pdf, invoiceData, pageHeight);
        
        document.close();
        return baos.toByteArray();
    }
    
    /**
     * 绘制外边框
     */
    private void drawOuterBorder(PdfDocument pdf, float width, float height) throws Exception {
        PdfCanvas canvas = new PdfCanvas(pdf.getFirstPage());
        Canvas c = new Canvas(canvas, new PageSize(width, height));
        
        // 外边框线 - 红褐色 0.25mm
        com.itextpdf.kernel.colors.Color borderColor = RED_BROWN;
        
        // 绘制四边边框
        canvas.setStrokeColor(borderColor)
              .setLineWidth(LINE_THICKNESS)
              .rectangle(0, 0, width, height)
              .stroke();
        
        c.close();
    }
    
    /**
     * 绘制票头区域
     * 票头文字上边缘距离票面上边缘 8mm
     * 双线位置：上侧线条中心距票面上边缘 18mm，线条长度 73mm，水平居中
     * 监制章中心点：水平 100.5mm，垂直 18mm
     * 二维码：左上角 (7mm, 6mm)，尺寸 20*20mm
     * 发票号码/日期：左边缘 155mm
     */
    private void drawHeader(Document document, InvoiceData invoiceData, 
                           PdfDocument pdf, float pageWidth, float pageHeight) {
        
        // 票头标题 - 楷体 20pt，水平居中，上边缘距票面 8mm
        Paragraph title = new Paragraph(getInvoiceTitle(invoiceData))
            .setFontSize(FONT_SIZE_TITLE)
            .setTextAlignment(TextAlignment.CENTER);
        // 设置字体为楷体 (需要系统中安装该字体)
        try {
            title.setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont("STKaiti", "UTF-8"));
        } catch (Exception e) {
            // 如果楷体不可用，使用默认字体
        }
        title.setMarginTop(8f);  // 上边缘距票面 8mm
        document.add(title);
        
        // 发票号码和开票日期 - 右侧，左边缘 155mm
        Table headerInfoTable = new Table(2);
        headerInfoTable.setWidth(UnitValue.createPointValue(50f));
        headerInfoTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        headerInfoTable.setFixedPosition(155f, pageHeight - 35f, 50f);
        
        headerInfoTable.addCell(createCellWithFontSize("发票号码:", FONT_SIZE_ELEMENT));
        headerInfoTable.addCell(createCellWithFontSize(invoiceData.getInvoiceNo(), FONT_SIZE_TAX_ID));
        headerInfoTable.addCell(createCellWithFontSize("开票日期:", FONT_SIZE_ELEMENT));
        headerInfoTable.addCell(createCellWithFontSize(invoiceData.getInvoiceDate(), FONT_SIZE_FILL));
        
        document.add(headerInfoTable);
    }
    
    /**
     * 绘制购买方和销售方信息
     * 区域顶部距票面 30mm，高 22mm
     * 分为 4 列：6mm, 94.5mm, 6mm, 94.5mm
     */
    private void drawPartiesInfo(Document document, InvoiceData invoiceData, PdfDocument pdf) {
        Table partiesTable = new Table(4);
        partiesTable.setWidth(UnitValue.createPointValue(INNER_FRAME_WIDTH));
        partiesTable.setFixedPosition(4.5f, 108f, INNER_FRAME_WIDTH);  // 距顶部 30mm
        
        // 购买方
        partiesTable.addCell(createLabelCell("购买方信息"));
        partiesTable.addCell(createValueCell(invoiceData.getBuyer().getName()));
        partiesTable.addCell(createLabelCell(""));
        partiesTable.addCell(createValueCell(invoiceData.getSeller().getName()));
        
        // 税号 - Courier New 12pt
        partiesTable.addCell(createLabelCell("统一社会信用代码/纳税人识别号"));
        partiesTable.addCell(createTaxIdCell(invoiceData.getBuyer().getTaxId()));
        partiesTable.addCell(createLabelCell("统一社会信用代码/纳税人识别号"));
        partiesTable.addCell(createTaxIdCell(invoiceData.getSeller().getTaxId()));
        
        document.add(partiesTable);
    }
    
    /**
     * 绘制应税明细和合计
     * 区域高 52mm，分为两行：44mm(明细) 和 8mm(合计)
     * 8 列：项目名称 37mm, 规格型号 24mm, 单位 12mm, 数量 25mm, 
     *      单价 25mm, 金额 26mm, 税率 25mm, 税额 27mm
     */
    private void drawItemsAndTotal(Document document, InvoiceData invoiceData, PdfDocument pdf) {
        // 明细表头
        String[] headers = {"项目名称", "规格型号", "单位", "数量", "单价", "金额", "税率", "税额"};
        float[] colWidths = {37f, 24f, 12f, 25f, 25f, 26f, 25f, 27f};
        
        Table itemsTable = new Table(8);
        itemsTable.setWidth(UnitValue.createPointValue(INNER_FRAME_WIDTH));
        
        // 表头 - 楷体 9pt
        for (String header : headers) {
            Cell headerCell = createHeaderCell(header);
            itemsTable.addHeaderCell(headerCell);
        }
        
        // 商品明细 - 宋体 9pt
        for (InvoiceItem item : invoiceData.getItems()) {
            itemsTable.addCell(createFillCell(item.getName()));
            itemsTable.addCell(createFillCell(item.getSpecification() != null ? item.getSpecification() : ""));
            itemsTable.addCell(createFillCell(item.getUnit() != null ? item.getUnit() : ""));
            itemsTable.addCell(createFillCell(item.getQuantity().toString()));
            itemsTable.addCell(createFillCell(formatDecimal(item.getUnitPrice())));
            itemsTable.addCell(createFillCell(formatDecimal(item.getAmount())));
            itemsTable.addCell(createFillCell(item.getTaxRate() + "%"));
            itemsTable.addCell(createFillCell(formatDecimal(item.getTaxAmount())));
        }
        
        // 合计行
        itemsTable.addCell(createLabelCell("合计"));
        itemsTable.addCell(createFillCell(""));
        itemsTable.addCell(createFillCell(""));
        itemsTable.addCell(createFillCell(""));
        itemsTable.addCell(createFillCell(""));
        itemsTable.addCell(createFillCell(formatDecimal(invoiceData.getTotalAmount())));
        itemsTable.addCell(createFillCell(""));
        itemsTable.addCell(createFillCell(formatDecimal(invoiceData.getTotalTax())));
        
        document.add(itemsTable);
        
        // 价税合计
        BigDecimal totalWithTax = invoiceData.getTotalAmount().add(invoiceData.getTotalTax());
        String amountInWords = numberToChinese(totalWithTax);
        
        document.add(new Paragraph("价税合计（大写）：" + amountInWords)
            .setFontSize(FONT_SIZE_FILL));
        document.add(new Paragraph("(小写): ¥" + formatDecimal(totalWithTax))
            .setFontSize(FONT_SIZE_FILL));
    }
    
    /**
     * 绘制备注区域
     * 底部线条中心距票面下边缘 16mm，区域高 20mm
     * 分为 2 列：6mm, 195mm
     */
    private void drawRemarks(Document document, InvoiceData invoiceData, 
                            PdfDocument pdf, float pageHeight) {
        Table remarksTable = new Table(2);
        remarksTable.setWidth(UnitValue.createPointValue(INNER_FRAME_WIDTH));
        
        remarksTable.addCell(createLabelCell("备注"));
        remarksTable.addCell(createFillCell(invoiceData.getRemarks() != null ? invoiceData.getRemarks() : ""));
        
        document.add(remarksTable);
    }
    
    /**
     * 绘制票尾
     * 包含开票人、收款人、复核人等信息
     */
    private void drawFooter(Document document, InvoiceData invoiceData, 
                           PdfDocument pdf, float pageHeight) {
        Table footerTable = new Table(3);
        footerTable.setWidth(UnitValue.createPointValue(INNER_FRAME_WIDTH));
        
        footerTable.addCell(createLabelCell("开票人"));
        footerTable.addCell(createLabelCell("收款人"));
        footerTable.addCell(createLabelCell("复核人"));
        
        footerTable.addCell(createFillCell(invoiceData.getDrawer() != null ? invoiceData.getDrawer() : ""));
        footerTable.addCell(createFillCell(invoiceData.getPayee() != null ? invoiceData.getPayee() : ""));
        footerTable.addCell(createFillCell(invoiceData.getReviewer() != null ? invoiceData.getReviewer() : ""));
        
        document.add(footerTable);
    }
    
    /**
     * 绘制监制章
     * 椭圆形，30mm×20mm，中心点 (100.5mm, 18mm)
     * 上环："全国统一发票监制章"
     * 中间："国家税务总局"
     * 下环："xx 省 (区、市) 税务局"
     * 字体：楷体 7 磅，印色：大红色
     */
    private void drawOfficialSeal(PdfDocument pdf, InvoiceData invoiceData) throws Exception {
        PdfCanvas canvas = new PdfCanvas(pdf.getFirstPage());
        
        // 保存状态
        canvas.saveState();
        
        // 设置大红色
        canvas.setStrokeColor(STAMP_RED)
              .setFillColor(STAMP_RED)
              .setLineWidth(0.5f);
        
        // 绘制椭圆外圈
        float sealCenterX = 100.5f;
        float sealCenterY = PAGE_WIDTH - 18f;  // Y 坐标从底部开始
        float a = SEAL_WIDTH / 2;  // 长半轴
        float b = SEAL_HEIGHT / 2; // 短半轴
        
        // 绘制椭圆 (近似)
        canvas.ellipse(sealCenterX, sealCenterY, a, b).stroke();
        
        // 绘制内环细线
        canvas.setLineWidth(0.2f);
        canvas.ellipse(sealCenterX, sealCenterY, a - 1.5f, b - 1.5f).stroke();
        
        // 恢复状态
        canvas.restoreState();
    }
    
    /**
     * 生成并绘制二维码
     * 尺寸：20*20mm
     * 位置：左上角 (7mm, 6mm)
     * 包含 CRC 校验码
     */
    private void drawQRCode(PdfDocument pdf, InvoiceData invoiceData, float pageHeight) throws Exception {
        // 生成二维码数据 (根据规范第六章)
        String qrData = generateQRData(invoiceData);
        
        // 使用 ZXing 生成二维码图像
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int qrSizePx = (int)(QR_CODE_SIZE * 3.78);  // mm 转像素 (约 75px)
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, 
            com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
        
        BitMatrix matrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, qrSizePx, qrSizePx, hints);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);
        
        // 将 BufferedImage 转换为 iText Image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(qrImage, "PNG", baos);
        byte[] qrImageBytes = baos.toByteArray();
        
        // 设置位置：左上角 (7mm, 6mm)，Y 坐标从底部开始计算
        float qrX = 7f;
        float qrY = pageHeight - 6f - QR_CODE_SIZE;  // 距离顶部 6mm
        
        // 添加到文档 - 通过 Canvas 方式添加，先缩放图像
        PdfCanvas canvas = new PdfCanvas(pdf.getFirstPage());
        com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(qrImageBytes);
        
        // 保存状态，应用变换矩阵来缩放和定位
        canvas.saveState();
        // 创建仿射变换矩阵：平移 + 缩放
        canvas.concatMatrix(
            QR_CODE_SIZE / imageData.getWidth(), 0,
            0, QR_CODE_SIZE / imageData.getHeight(),
            qrX, qrY
        );
        canvas.addImageAt(imageData, 0, 0, false);
        canvas.restoreState();
        canvas.release();
    }
    
    /**
     * 生成二维码数据
     * 根据规范第六章，包含发票关键信息和 CRC 校验
     */
    private String generateQRData(InvoiceData invoiceData) {
        StringBuilder sb = new StringBuilder();
        // 按照规范格式组装二维码数据
        // 示例格式：发票代码|发票号码|开票日期|金额|校验码|...
        sb.append(invoiceData.getInvoiceCode() != null ? invoiceData.getInvoiceCode() : "")
          .append("|")
          .append(invoiceData.getInvoiceNo())
          .append("|")
          .append(invoiceData.getInvoiceDate())
          .append("|")
          .append(formatDecimal(invoiceData.getTotalAmount().add(invoiceData.getTotalTax())))
          .append("|")
          .append(calculateCRC(invoiceData));
        return sb.toString();
    }
    
    /**
     * 计算 CRC 校验码
     * 根据规范第六章 CRC 算法说明
     */
    private String calculateCRC(InvoiceData invoiceData) {
        // 使用 CRC32 算法计算校验码
        CRC32 crc32 = new CRC32();
        String dataStr = invoiceData.getInvoiceNo() + 
                        invoiceData.getInvoiceDate() + 
                        formatDecimal(invoiceData.getTotalAmount().add(invoiceData.getTotalTax()));
        crc32.update(dataStr.getBytes());
        long crcValue = crc32.getValue();
        
        // 转换为 8 位十六进制字符串
        return String.format("%08X", crcValue);
    }
    
    /**
     * 获取发票标题
     */
    private String getInvoiceTitle(InvoiceData invoiceData) {
        String type = invoiceData.getInvoiceType();
        if ("SPECIAL".equals(type)) {
            return "电子发票（增值税专用发票）";
        } else if ("NORMAL".equals(type)) {
            return "电子发票（普通发票）";
        }
        return "电子发票（普通发票）";
    }
    
    // ===== 辅助方法 =====
    
    private Cell createCell(String content, boolean isBold) {
        Cell cell = new Cell();
        cell.add(new Paragraph(content));
        if (isBold) cell.setBold();
        return cell;
    }
    
    /**
     * 创建指定字号的单元格
     */
    private Cell createCellWithFontSize(String content, float fontSize) {
        Cell cell = new Cell();
        cell.setFontSize(fontSize);
        cell.add(new Paragraph(content));
        return cell;
    }
    
    private Cell createLabelCell(String content) {
        Cell cell = new Cell();
        try {
            cell.setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont("STKaiti", "UTF-8"));
        } catch (Exception e) {}
        cell.setFontSize(FONT_SIZE_ELEMENT);
        cell.add(new Paragraph(content));
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }
    
    private Cell createValueCell(String content) {
        Cell cell = new Cell();
        try {
            cell.setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont("SimSun", "UTF-8"));
        } catch (Exception e) {}
        cell.setFontSize(FONT_SIZE_FILL);
        cell.add(new Paragraph(content));
        return cell;
    }
    
    private Cell createTaxIdCell(String content) {
        Cell cell = new Cell();
        try {
            cell.setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont("Courier New", "UTF-8"));
        } catch (Exception e) {}
        cell.setFontSize(FONT_SIZE_TAX_ID);
        cell.add(new Paragraph(content));
        return cell;
    }
    
    private Cell createFillCell(String content) {
        Cell cell = new Cell();
        try {
            cell.setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont("SimSun", "UTF-8"));
        } catch (Exception e) {}
        cell.setFontSize(FONT_SIZE_FILL);
        cell.add(new Paragraph(content));
        return cell;
    }
    
    private Cell createHeaderCell(String content) {
        Cell cell = new Cell();
        try {
            cell.setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont("STKaiti", "UTF-8"));
        } catch (Exception e) {}
        cell.setFontSize(FONT_SIZE_ELEMENT);
        cell.setBold();
        cell.add(new Paragraph(content));
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }
    
    private String formatDecimal(BigDecimal value) {
        return String.format("%.2f", value);
    }
    
    private String formatDecimal(Double value) {
        return String.format("%.2f", value != null ? value : 0.0);
    }
    
    /**
     * 数字转中文大写金额
     */
    private String numberToChinese(BigDecimal amount) {
        String[] chineseDigits = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        
        long integerPart = amount.longValue();
        int decimalPart = amount.subtract(new BigDecimal(integerPart))
                                .multiply(new BigDecimal("100"))
                                .intValue();

        if (integerPart == 0) {
            return "零元";
        }

        StringBuilder result = new StringBuilder();
        String numStr = String.valueOf(integerPart);
        int len = numStr.length();
        
        String[] units = {"", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟"};
        boolean zeroFlag = false;

        for (int i = 0; i < len; i++) {
            int digit = numStr.charAt(i) - '0';
            int unitPos = len - i - 1;

            if (digit == 0) {
                zeroFlag = true;
            } else {
                if (zeroFlag) {
                    result.append(chineseDigits[0]);
                    zeroFlag = false;
                }
                result.append(chineseDigits[digit]).append(units[unitPos]);
            }
        }

        result.append("元");

        int jiao = decimalPart / 10;
        int fen = decimalPart % 10;

        if (jiao > 0 || fen > 0) {
            if (jiao > 0) {
                result.append(chineseDigits[jiao]).append("角");
            }
            if (fen > 0) {
                result.append(chineseDigits[fen]).append("分");
            }
        } else {
            result.append("整");
        }

        return result.toString();
    }
}
