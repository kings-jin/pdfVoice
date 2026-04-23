package com.example.einvoice.generator;

import com.example.einvoice.model.InvoiceData;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 发票 OFD 生成器 - 基于 V6.001 规范
 * 
 * 注意：OFD 是中国国家标准 (GB/T 33190-2016) 的版式文档格式
 * 完整的 OFD 生成需要专门的库支持，这里提供一个简化版本
 * 实际生产环境建议使用专业的 OFD 生成库或第三方服务
 */
@Component
public class InvoiceOFDGenerator {

    /**
     * 生成简化的 OFD 文件（ZIP 格式包装）
     * 
     * OFD 文件本质是一个 ZIP 包，包含 XML 描述文件和资源文件
     * 完整实现需要遵循 GB/T 33190-2016 标准
     */
    public byte[] generate(InvoiceData invoiceData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        // 添加 OFD 文档结构
        // 1. 文档根目录描述文件
        addZipEntry(zos, "OFD.xml", createOfdRootXml());
        
        // 2. 文档页面描述
        addZipEntry(zos, "Doc_0/Page_1/Content.xml", createPageContent(invoiceData));
        
        // 3. 公共资源文件
        addZipEntry(zos, "Doc_0/Res.xml", createResources());
        
        // 4. 文档元数据
        addZipEntry(zos, "Doc_0/Document.xml", createDocumentMeta(invoiceData));

        zos.close();
        return baos.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zos, String name, String content) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private String createOfdRootXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" Version=\"1.0\">\n" +
               "  <ofd:DocBody>\n" +
               "    <ofd:DocInfo>\n" +
               "      <ofd:Author>电子发票系统</ofd:Author>\n" +
               "    </ofd:DocInfo>\n" +
               "    <ofd:Pages>\n" +
               "      <ofd:Page BaseLoc=\"Doc_0/Page_1/Content.xml\"/>\n" +
               "    </ofd:Pages>\n" +
               "  </ofd:DocBody>\n" +
               "</ofd:OFD>";
    }

    private String createPageContent(InvoiceData invoiceData) {
        StringBuilder content = new StringBuilder();
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        content.append("<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\" Width=\"210mm\" Height=\"297mm\">\n");
        content.append("  <ofd:Layer>\n");
        
        // 标题
        content.append("    <ofd:TextBlock x=\"50\" y=\"30\" fontSize=\"18\" font=\"SimSun\" bold=\"true\">");
        content.append("电子发票（普通发票）");
        content.append("</ofd:TextBlock>\n");
        
        // 发票信息
        int y = 60;
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("发票号码：").append(invoiceData.getInvoiceNo());
        content.append("</ofd:TextBlock>\n");
        
        y += 20;
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("开票日期：").append(invoiceData.getInvoiceDate());
        content.append("</ofd:TextBlock>\n");
        
        // 购买方信息
        y += 30;
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"11\" bold=\"true\">购买方信息:</ofd:TextBlock>\n");
        y += 15;
        content.append("    <ofd:TextBlock x=\"60\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("名称：").append(invoiceData.getBuyer().getName());
        content.append("</ofd:TextBlock>\n");
        
        y += 15;
        content.append("    <ofd:TextBlock x=\"60\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("统一社会信用代码/纳税人识别号：").append(invoiceData.getBuyer().getTaxId());
        content.append("</ofd:TextBlock>\n");
        
        // 销售方信息
        y += 30;
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"11\" bold=\"true\">销售方信息:</ofd:TextBlock>\n");
        y += 15;
        content.append("    <ofd:TextBlock x=\"60\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("名称：").append(invoiceData.getSeller().getName());
        content.append("</ofd:TextBlock>\n");
        
        y += 15;
        content.append("    <ofd:TextBlock x=\"60\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("统一社会信用代码/纳税人识别号：").append(invoiceData.getSeller().getTaxId());
        content.append("</ofd:TextBlock>\n");
        
        // 商品明细
        y += 30;
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"11\" bold=\"true\">商品明细:</ofd:TextBlock>\n");
        
        y += 20;
        content.append("    <ofd:TextBlock x=\"60\" y=\"").append(y).append("\" fontSize=\"9\">");
        content.append("项目名称          金额        税率      税额");
        content.append("</ofd:TextBlock>\n");
        
        y += 15;
        for (var item : invoiceData.getItems()) {
            content.append("    <ofd:TextBlock x=\"60\" y=\"").append(y).append("\" fontSize=\"9\">");
            content.append(item.getName()).append("   ")
                    .append(String.format("%.2f", item.getAmount())).append("   ")
                    .append(item.getTaxRate()).append("%   ")
                    .append(String.format("%.2f", item.getTaxAmount()));
            content.append("</ofd:TextBlock>\n");
            y += 15;
        }
        
        // 合计
        y += 15;
        content.append("    <ofd:TextBlock x=\"60\" y=\"").append(y).append("\" fontSize=\"10\" bold=\"true\">");
        content.append("合计：").append(String.format("%.2f", invoiceData.getTotalAmount()))
              .append("   税额：").append(String.format("%.2f", invoiceData.getTotalTax()));
        content.append("</ofd:TextBlock>\n");
        
        // 价税合计
        y += 20;
        var totalWithTax = invoiceData.getTotalAmount().add(invoiceData.getTotalTax());
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("价税合计（大写）：").append(numberToChinese(totalWithTax));
        content.append("</ofd:TextBlock>\n");
        
        y += 15;
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("（小写）：¥").append(String.format("%.2f", totalWithTax));
        content.append("</ofd:TextBlock>\n");
        
        // 开票人
        y += 30;
        content.append("    <ofd:TextBlock x=\"50\" y=\"").append(y).append("\" fontSize=\"10\">");
        content.append("开票人：").append(invoiceData.getDrawer());
        content.append("</ofd:TextBlock>\n");
        
        content.append("  </ofd:Layer>\n");
        content.append("</ofd:Page>");
        
        return content.toString();
    }

    private String createResources() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ofd:Resources xmlns:ofd=\"http://www.ofdspec.org/2016\">\n" +
               "  <ofd:Fonts>\n" +
               "    <ofd:Font ID=\"0\" Name=\"SimSun\" FamilyName=\"宋体\"/>\n" +
               "  </ofd:Fonts>\n" +
               "</ofd:Resources>";
    }

    private String createDocumentMeta(InvoiceData invoiceData) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\">\n" +
               "  <ofd:DocID>" + invoiceData.getInvoiceNo() + "</ofd:DocID>\n" +
               "  <ofd:Title>电子发票</ofd:Title>\n" +
               "  <ofd:Author>" + invoiceData.getSeller().getName() + "</ofd:Author>\n" +
               "  <ofd:CreationDate>" + invoiceData.getInvoiceDate() + "</ofd:CreationDate>\n" +
               "</ofd:Document>";
    }

    /**
     * 数字转中文大写金额
     */
    private String numberToChinese(java.math.BigDecimal amount) {
        String[] chineseDigits = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        
        long integerPart = amount.longValue();
        int decimalPart = amount.subtract(new java.math.BigDecimal(integerPart))
                                .multiply(new java.math.BigDecimal("100"))
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
