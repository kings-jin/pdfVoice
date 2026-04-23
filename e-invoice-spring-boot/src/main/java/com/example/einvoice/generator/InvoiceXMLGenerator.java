package com.example.einvoice.generator;

import com.example.einvoice.model.InvoiceData;
import com.example.einvoice.model.InvoiceItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 发票 XML 生成器 - 符合 V6.001 规范
 */
@Component
public class InvoiceXMLGenerator {

    private static final String NAMESPACE = "http://www.chinatax.gov.cn/2019/InvoiceML";

    /**
     * 生成发票 XML
     */
    public String generate(InvoiceData invoiceData) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Invoice xmlns=\"").append(NAMESPACE).append("\">\n");

        // 发票基本信息
        xml.append("  <Header>\n");
        xml.append("    <InvoiceCode>").append(escapeXml(invoiceData.getInvoiceCode())).append("</InvoiceCode>\n");
        xml.append("    <InvoiceNo>").append(escapeXml(invoiceData.getInvoiceNo())).append("</InvoiceNo>\n");
        xml.append("    <IssueDate>").append(escapeXml(invoiceData.getInvoiceDate())).append("</IssueDate>\n");
        xml.append("    <CheckCode>").append(escapeXml(invoiceData.getCheckCode())).append("</CheckCode>\n");
        xml.append("    <MachineNo>").append(escapeXml(invoiceData.getMachineNo())).append("</MachineNo>\n");
        xml.append("  </Header>\n");

        // 销售方信息
        xml.append("  <SellerInfo>\n");
        xml.append("    <SellerName>").append(escapeXml(invoiceData.getSeller().getName())).append("</SellerName>\n");
        xml.append("    <SellerTaxID>").append(escapeXml(invoiceData.getSeller().getTaxId())).append("</SellerTaxID>\n");
        xml.append("    <SellerAddress>").append(escapeXml(invoiceData.getSeller().getAddress())).append("</SellerAddress>\n");
        xml.append("    <SellerPhone>").append(escapeXml(invoiceData.getSeller().getPhone())).append("</SellerPhone>\n");
        xml.append("    <SellerBankName>").append(escapeXml(invoiceData.getSeller().getBankName())).append("</SellerBankName>\n");
        xml.append("    <SellerBankAccount>").append(escapeXml(invoiceData.getSeller().getBankAccount())).append("</SellerBankAccount>\n");
        xml.append("  </SellerInfo>\n");

        // 购买方信息
        xml.append("  <BuyerInfo>\n");
        xml.append("    <BuyerName>").append(escapeXml(invoiceData.getBuyer().getName())).append("</BuyerName>\n");
        xml.append("    <BuyerTaxID>").append(escapeXml(invoiceData.getBuyer().getTaxId())).append("</BuyerTaxID>\n");
        xml.append("    <BuyerAddress>").append(escapeXml(invoiceData.getBuyer().getAddress())).append("</BuyerAddress>\n");
        xml.append("    <BuyerPhone>").append(escapeXml(invoiceData.getBuyer().getPhone())).append("</BuyerPhone>\n");
        xml.append("    <BuyerBankName>").append(escapeXml(invoiceData.getBuyer().getBankName())).append("</BuyerBankName>\n");
        xml.append("    <BuyerBankAccount>").append(escapeXml(invoiceData.getBuyer().getBankAccount())).append("</BuyerBankAccount>\n");
        xml.append("  </BuyerInfo>\n");

        // 商品明细
        xml.append("  <Items>\n");
        for (int i = 0; i < invoiceData.getItems().size(); i++) {
            InvoiceItem item = invoiceData.getItems().get(i);
            xml.append("    <Item>\n");
            xml.append("      <ItemIndex>").append(i + 1).append("</ItemIndex>\n");
            xml.append("      <ItemName>").append(escapeXml(item.getName())).append("</ItemName>\n");
            xml.append("      <Specification>").append(escapeXml(item.getSpecification())).append("</Specification>\n");
            xml.append("      <Unit>").append(escapeXml(item.getUnit())).append("</Unit>\n");
            xml.append("      <Quantity>").append(item.getQuantity()).append("</Quantity>\n");
            xml.append("      <UnitPrice>").append(item.getUnitPrice()).append("</UnitPrice>\n");
            xml.append("      <Amount>").append(item.getAmount()).append("</Amount>\n");
            xml.append("      <TaxRate>").append(item.getTaxRate()).append("</TaxRate>\n");
            xml.append("      <TaxAmount>").append(item.getTaxAmount()).append("</TaxAmount>\n");
            xml.append("    </Item>\n");
        }
        xml.append("  </Items>\n");

        // 合计信息
        xml.append("  <Totals>\n");
        xml.append("    <TotalAmount>").append(invoiceData.getTotalAmount()).append("</TotalAmount>\n");
        xml.append("    <TotalTax>").append(invoiceData.getTotalTax()).append("</TotalTax>\n");
        xml.append("    <TotalAmountWithTax>").append(invoiceData.getTotalAmount().add(invoiceData.getTotalTax())).append("</TotalAmountWithTax>\n");
        xml.append("    <AmountInWords>").append(numberToChinese(invoiceData.getTotalAmount().add(invoiceData.getTotalTax()))).append("</AmountInWords>\n");
        xml.append("  </Totals>\n");

        // 其他信息
        xml.append("  <OtherInfo>\n");
        xml.append("    <Remarks>").append(escapeXml(invoiceData.getRemarks())).append("</Remarks>\n");
        xml.append("    <Drawer>").append(escapeXml(invoiceData.getDrawer())).append("</Drawer>\n");
        xml.append("    <Payee>").append(escapeXml(invoiceData.getPayee())).append("</Payee>\n");
        xml.append("    <Reviewer>").append(escapeXml(invoiceData.getReviewer())).append("</Reviewer>\n");
        xml.append("    <IssueTime>").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</IssueTime>\n");
        xml.append("  </OtherInfo>\n");

        xml.append("</Invoice>");
        return xml.toString();
    }

    /**
     * XML 特殊字符转义
     */
    private String escapeXml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
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
