package com.example.einvoice.model;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发票数据模型 - 基于数字化电子发票版式及 XML 规范 V6.001
 */
@Data
public class InvoiceData {

    /**
     * 发票代码 (12 位)
     */
    private String invoiceCode;

    /**
     * 发票号码 (20 位)
     */
    @NotBlank(message = "发票号码不能为空")
    private String invoiceNo;

    /**
     * 校验码 (20 位)
     */
    private String checkCode;

    /**
     * 机器编号 (12 位)
     */
    private String machineNo;

    /**
     * 开票日期 (格式：yyyy 年 MM 月 dd 日)
     */
    @NotBlank(message = "开票日期不能为空")
    private String invoiceDate;

    /**
     * 销售方信息
     */
    @NotNull(message = "销售方信息不能为空")
    @Valid
    private PartyInfo seller;

    /**
     * 购买方信息
     */
    @NotNull(message = "购买方信息不能为空")
    @Valid
    private PartyInfo buyer;

    /**
     * 商品明细列表
     */
    @NotNull(message = "商品明细不能为空")
    @Valid
    private List<InvoiceItem> items;

    /**
     * 合计金额 (不含税)
     */
    @NotNull(message = "合计金额不能为空")
    private BigDecimal totalAmount;

    /**
     * 合计税额
     */
    @NotNull(message = "合计税额不能为空")
    private BigDecimal totalTax;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 开票人
     */
    @NotBlank(message = "开票人不能为空")
    private String drawer;

    /**
     * 收款人
     */
    private String payee;

    /**
     * 复核人
     */
    private String reviewer;
    
    /**
     * 发票类型 (NORMAL: 普通发票，SPECIAL: 专用发票)
     */
    private String invoiceType = "NORMAL";
}
