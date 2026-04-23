package com.example.einvoice.service;

import com.example.einvoice.generator.InvoiceOFDGenerator;
import com.example.einvoice.generator.InvoicePDFGenerator;
import com.example.einvoice.generator.InvoiceXMLGenerator;
import com.example.einvoice.model.InvoiceData;
import com.example.einvoice.model.InvoiceGenerationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 发票生成服务
 */
@Service
public class InvoiceService {

    @Autowired
    private InvoiceXMLGenerator xmlGenerator;

    @Autowired
    private InvoiceOFDGenerator ofdGenerator;

    @Autowired
    private InvoicePDFGenerator pdfGenerator;

    // 内存存储生成的文件（生产环境应使用文件系统或对象存储）
    private final Map<String, byte[]> fileStorage = new HashMap<>();

    /**
     * 生成发票文件
     *
     * @param invoiceData 发票数据
     * @param formats     需要生成的格式 (xml, ofd, pdf)
     * @return 生成结果
     */
    public InvoiceGenerationResult generateInvoice(InvoiceData invoiceData, String[] formats) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String invoiceNo = invoiceData.getInvoiceNo();
        
        try {
            String xmlPath = null;
            String ofdPath = null;
            String pdfPath = null;

            // 生成 XML
            if (contains(formats, "xml")) {
                String xmlContent = xmlGenerator.generate(invoiceData);
                xmlPath = generateFilePath(invoiceNo, timestamp, "xml");
                fileStorage.put(xmlPath, xmlContent.getBytes("UTF-8"));
            }

            // 生成 OFD
            if (contains(formats, "ofd")) {
                byte[] ofdBytes = ofdGenerator.generate(invoiceData);
                ofdPath = generateFilePath(invoiceNo, timestamp, "ofd");
                fileStorage.put(ofdPath, ofdBytes);
            }

            // 生成 PDF
            if (contains(formats, "pdf")) {
                byte[] pdfBytes = pdfGenerator.generate(invoiceData);
                pdfPath = generateFilePath(invoiceNo, timestamp, "pdf");
                fileStorage.put(pdfPath, pdfBytes);
            }

            return InvoiceGenerationResult.success(invoiceNo, xmlPath, ofdPath, pdfPath, timestamp);

        } catch (Exception e) {
            e.printStackTrace();
            return InvoiceGenerationResult.failure(invoiceNo, "生成失败：" + e.getMessage());
        }
    }

    /**
     * 获取生成的文件内容
     */
    public byte[] getFileContent(String filePath) {
        return fileStorage.get(filePath);
    }

    /**
     * 检查数组是否包含指定元素
     */
    private boolean contains(String[] array, String value) {
        if (array == null || array.length == 0) {
            return false;
        }
        for (String item : array) {
            if (value.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成文件路径
     */
    private String generateFilePath(String invoiceNo, String timestamp, String extension) {
        return String.format("invoices/%s_%s.%s", invoiceNo, timestamp, extension);
    }

    /**
     * 清理过期的文件（可选，用于定时任务）
     */
    public void cleanupOldFiles(LocalDateTime beforeTime) {
        // 实现文件清理逻辑
        // 生产环境建议使用文件系统或对象存储，并设置过期策略
    }
}
