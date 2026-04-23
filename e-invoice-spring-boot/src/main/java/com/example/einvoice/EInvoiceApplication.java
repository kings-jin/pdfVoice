package com.example.einvoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数字化电子发票生成系统
 * 基于《数字化电子发票版式及 XML 规范 V6.001》
 * 
 * 支持生成：
 * - XML: 符合国家税务总局规范的发票数据文件
 * - OFD: 版式文件（Open Fixed-layout Document，中国国家标准 GB/T 33190-2016）
 * - PDF: 便携式文档格式
 */
@SpringBootApplication
public class EInvoiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EInvoiceApplication.class, args);
        System.out.println("=================================================");
        System.out.println("  数字化电子发票生成系统已启动");
        System.out.println("  版本：1.0.0");
        System.out.println("  基于规范：V6.001");
        System.out.println("=================================================");
        System.out.println("API 接口:");
        System.out.println("  POST /api/invoices/generate - 生成发票");
        System.out.println("  GET  /api/invoices/download/xml - 下载 XML");
        System.out.println("  GET  /api/invoices/download/ofd - 下载 OFD");
        System.out.println("  GET  /api/invoices/download/pdf - 下载 PDF");
        System.out.println("  GET  /api/invoices/health - 健康检查");
        System.out.println("=================================================");
    }
}
