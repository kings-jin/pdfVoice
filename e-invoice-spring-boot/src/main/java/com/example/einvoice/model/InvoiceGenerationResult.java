package com.example.einvoice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发票生成结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceGenerationResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * XML 文件路径
     */
    private String xmlPath;

    /**
     * OFD 文件路径
     */
    private String ofdPath;

    /**
     * PDF 文件路径
     */
    private String pdfPath;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 时间戳
     */
    private String timestamp;

    public static InvoiceGenerationResult success(String invoiceNo, String xmlPath, String ofdPath, String pdfPath, String timestamp) {
        return new InvoiceGenerationResult(true, invoiceNo, xmlPath, ofdPath, pdfPath, null, timestamp);
    }

    public static InvoiceGenerationResult failure(String invoiceNo, String errorMessage) {
        return new InvoiceGenerationResult(false, invoiceNo, null, null, null, errorMessage, null);
    }
}
