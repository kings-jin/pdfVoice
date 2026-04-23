package com.example.einvoice.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 发票商品明细项
 */
@Data
public class InvoiceItem {

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    private String name;

    /**
     * 规格型号
     */
    private String specification;

    /**
     * 单位
     */
    private String unit;

    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于 0")
    private Integer quantity;

    /**
     * 单价 (不含税)
     */
    @NotNull(message = "单价不能为空")
    @Positive(message = "单价必须大于 0")
    private BigDecimal unitPrice;

    /**
     * 金额 (不含税) = 数量 * 单价
     */
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于 0")
    private BigDecimal amount;

    /**
     * 税率 (%)
     */
    @NotNull(message = "税率不能为空")
    @Positive(message = "税率必须大于 0")
    private BigDecimal taxRate;

    /**
     * 税额 = 金额 * 税率
     */
    @NotNull(message = "税额不能为空")
    @Positive(message = "税额必须大于 0")
    private BigDecimal taxAmount;
}
