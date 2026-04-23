package com.example.einvoice.generator;

import com.example.einvoice.model.InvoiceData;
import com.example.einvoice.model.InvoiceItem;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/**
 * 发票 PDF 生成器 - 基于 V6.001 规范版式
 */
@Component
public class InvoicePDFGenerator {

    /**
     * 生成发票 PDF
     */
    public byte[] generate(InvoiceData invoiceData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // 标题
        Paragraph title = new Paragraph("电子发票（普通发票）")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        document.add(new Paragraph("\n"));

        // 发票基本信息表格
        Table infoTable = new Table(4);
        infoTable.setWidth(UnitValue.createPercentValue(100));
        
        infoTable.addCell(createCell("发票号码：", true));
        infoTable.addCell(createCell(invoiceData.getInvoiceNo(), false, 3));
        
        infoTable.addCell(createCell("开票日期：", true));
        infoTable.addCell(createCell(invoiceData.getInvoiceDate(), false, 3));

        document.add(infoTable);
        document.add(new Paragraph("\n"));

        // 购买方信息
        document.add(new Paragraph("购买方信息:").setFontSize(12).setBold());
        Table buyerTable = new Table(2);
        buyerTable.setWidth(UnitValue.createPercentValue(100));
        buyerTable.addCell(createCell("名称：", true));
        buyerTable.addCell(createCell(invoiceData.getBuyer().getName(), false));
        buyerTable.addCell(createCell("统一社会信用代码/纳税人识别号：", true));
        buyerTable.addCell(createCell(invoiceData.getBuyer().getTaxId(), false));
        if (invoiceData.getBuyer().getAddress() != null && !invoiceData.getBuyer().getAddress().isEmpty()) {
            buyerTable.addCell(createCell("地址：", true));
            buyerTable.addCell(createCell(invoiceData.getBuyer().getAddress(), false));
        }
        if (invoiceData.getBuyer().getPhone() != null && !invoiceData.getBuyer().getPhone().isEmpty()) {
            buyerTable.addCell(createCell("电话：", true));
            buyerTable.addCell(createCell(invoiceData.getBuyer().getPhone(), false));
        }
        document.add(buyerTable);
        document.add(new Paragraph("\n"));

        // 销售方信息
        document.add(new Paragraph("销售方信息:").setFontSize(12).setBold());
        Table sellerTable = new Table(2);
        sellerTable.setWidth(UnitValue.createPercentValue(100));
        sellerTable.addCell(createCell("名称：", true));
        sellerTable.addCell(createCell(invoiceData.getSeller().getName(), false));
        sellerTable.addCell(createCell("统一社会信用代码/纳税人识别号：", true));
        sellerTable.addCell(createCell(invoiceData.getSeller().getTaxId(), false));
        if (invoiceData.getSeller().getAddress() != null && !invoiceData.getSeller().getAddress().isEmpty()) {
            sellerTable.addCell(createCell("地址：", true));
            sellerTable.addCell(createCell(invoiceData.getSeller().getAddress(), false));
        }
        if (invoiceData.getSeller().getPhone() != null && !invoiceData.getSeller().getPhone().isEmpty()) {
            sellerTable.addCell(createCell("电话：", true));
            sellerTable.addCell(createCell(invoiceData.getSeller().getPhone(), false));
        }
        document.add(sellerTable);
        document.add(new Paragraph("\n"));

        // 商品明细表格
        document.add(new Paragraph("商品明细:").setFontSize(12).setBold());
        Table itemsTable = new Table(5);
        itemsTable.setWidth(UnitValue.createPercentValue(100));
        
        // 表头
        itemsTable.addHeaderCell(createHeaderCell("项目名称"));
        itemsTable.addHeaderCell(createHeaderCell("金额"));
        itemsTable.addHeaderCell(createHeaderCell("税率"));
        itemsTable.addHeaderCell(createHeaderCell("税额"));
        
        // 商品列表
        for (InvoiceItem item : invoiceData.getItems()) {
            itemsTable.addCell(createCell(item.getName(), false));
            itemsTable.addCell(createCell(formatDecimal(item.getAmount()), false));
            itemsTable.addCell(createCell(item.getTaxRate() + "%", false));
            itemsTable.addCell(createCell(formatDecimal(item.getTaxAmount()), false));
        }
        
        // 合计行
        itemsTable.addCell(createCell("合计", true));
        itemsTable.addCell(createCell(formatDecimal(invoiceData.getTotalAmount()), true));
        itemsTable.addCell(createCell("", false));
        itemsTable.addCell(createCell(formatDecimal(invoiceData.getTotalTax()), true));
        
        document.add(itemsTable);
        document.add(new Paragraph("\n"));

        // 价税合计
        BigDecimal totalWithTax = invoiceData.getTotalAmount().add(invoiceData.getTotalTax());
        String amountInWords = numberToChinese(totalWithTax);
        
        document.add(new Paragraph("价税合计（大写）：" + amountInWords));
        document.add(new Paragraph("（小写）：¥" + formatDecimal(totalWithTax)));
        document.add(new Paragraph("\n"));

        // 开票人
        document.add(new Paragraph("开票人：" + invoiceData.getDrawer()));

        document.close();
        return baos.toByteArray();
    }

    private Cell createCell(String content, boolean isHeader) {
        Cell cell = new Cell();
        cell.add(new Paragraph(content));
        if (isHeader) {
            cell.setBold();
        }
        return cell;
    }

    private Cell createCell(String content, boolean isHeader, int colspan) {
        Cell cell = new Cell();
        cell.add(new Paragraph(content));
        if (isHeader) {
            cell.setBold();
        }
        if (colspan > 1) {
            cell.setProperty(com.itextpdf.layout.properties.Property.COLSPAN, colspan);
        }
        return cell;
    }

    private Cell createHeaderCell(String content) {
        Cell cell = new Cell();
        cell.add(new Paragraph(content).setBold());
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }

    private String formatDecimal(BigDecimal value) {
        return String.format("%.2f", value);
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
