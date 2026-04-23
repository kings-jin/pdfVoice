package com.example.einvoice.controller;

import com.example.einvoice.model.InvoiceData;
import com.example.einvoice.model.InvoiceGenerationResult;
import com.example.einvoice.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 发票生成控制器
 * 
 * 提供 RESTful API 接口用于生成和下载发票文件
 */
@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    /**
     * 生成并直接下载 PDF 发票
     * 
     * POST /api/invoices/generate/pdf
     * Content-Type: application/json
     * 
     * 直接返回 PDF 文件流，浏览器会自动下载
     */
    @PostMapping("/generate/pdf")
    public ResponseEntity<byte[]> generateAndDownloadPdf(
            @Validated @RequestBody InvoiceData invoiceData) {
        
        try {
            byte[] pdfContent = invoiceService.generatePdfContent(invoiceData);
            
            String fileName = "Invoice_" + invoiceData.getInvoiceNo() + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
    
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdfContent.length)
                    .body(pdfContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 生成并直接下载 XML 发票
     * 
     * POST /api/invoices/generate/xml
     * Content-Type: application/json
     * 
     * 直接返回 XML 文件流，浏览器会自动下载
     */
    @PostMapping("/generate/xml")
    public ResponseEntity<byte[]> generateAndDownloadXml(
            @Validated @RequestBody InvoiceData invoiceData) {
        
        try {
            byte[] xmlContent = invoiceService.generateXmlContent(invoiceData);
            
            String fileName = "Invoice_" + invoiceData.getInvoiceNo() + ".xml";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
    
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(xmlContent.length)
                    .body(xmlContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 生成并直接下载 OFD 发票
     * 
     * POST /api/invoices/generate/ofd
     * Content-Type: application/json
     * 
     * 直接返回 OFD 文件流，浏览器会自动下载
     */
    @PostMapping("/generate/ofd")
    public ResponseEntity<byte[]> generateAndDownloadOfd(
            @Validated @RequestBody InvoiceData invoiceData) {
        
        try {
            byte[] ofdContent = invoiceService.generateOfdContent(invoiceData);
            
            String fileName = "Invoice_" + invoiceData.getInvoiceNo() + ".ofd";
            
            MediaType ofdMediaType = MediaType.parseMediaType("application/ofd");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(ofdMediaType);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
    
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(ofdContent.length)
                    .body(ofdContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 生成发票（返回文件路径）
     * 
     * POST /api/invoices/generate
     * Content-Type: application/json
     * 
     * Body:
     * {
     *   "invoiceNo": "26532000000566953411",
     *   "invoiceDate": "2024 年 04 月 23 日",
     *   "seller": {...},
     *   "buyer": {...},
     *   "items": [...],
     *   "totalAmount": 14150.94,
     *   "totalTax": 849.06,
     *   "drawer": "汤琼"
     * }
     * 
     * Query Parameters:
     * - formats: 需要生成的格式，逗号分隔 (xml,ofd,pdf)，默认全部生成
     */
    @PostMapping("/generate")
    public ResponseEntity<InvoiceGenerationResult> generateInvoice(
            @Validated @RequestBody InvoiceData invoiceData,
            @RequestParam(required = false, defaultValue = "xml,ofd,pdf") String formats) {
        
        String[] formatArray = formats.split(",");
        InvoiceGenerationResult result = invoiceService.generateInvoice(invoiceData, formatArray);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 下载 XML 文件
     * 
     * GET /api/invoices/download/xml?path=invoices/26532000000566953411_20240423152703.xml
     */
    @GetMapping("/download/xml")
    public ResponseEntity<byte[]> downloadXml(@RequestParam String path) {
        return downloadFile(path, MediaType.APPLICATION_XML, "xml");
    }

    /**
     * 下载 OFD 文件
     * 
     * GET /api/invoices/download/ofd?path=invoices/26532000000566953411_20240423152703.ofd
     */
    @GetMapping("/download/ofd")
    public ResponseEntity<byte[]> downloadOfd(@RequestParam String path) {
        // OFD 文件的 MIME 类型
        MediaType ofdMediaType = MediaType.parseMediaType("application/ofd");
        return downloadFile(path, ofdMediaType, "ofd");
    }

    /**
     * 下载 PDF 文件
     * 
     * GET /api/invoices/download/pdf?path=invoices/26532000000566953411_20240423152703.pdf
     */
    @GetMapping("/download/pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestParam String path) {
        return downloadFile(path, MediaType.APPLICATION_PDF, "pdf");
    }

    /**
     * 通用文件下载方法
     */
    private ResponseEntity<byte[]> downloadFile(String path, MediaType mediaType, String extension) {
        byte[] content = invoiceService.getFileContent(path);
        
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        // 从路径中提取文件名
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(content.length)
                .body(content);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "e-invoice",
                "version", "1.0.0"
        ));
    }
}
