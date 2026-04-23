package com.example.einvoice.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 交易方信息模型（销售方/购买方）
 */
@Data
public class PartyInfo {

    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 统一社会信用代码/纳税人识别号 (18 位)
     */
    @NotBlank(message = "统一社会信用代码不能为空")
    @Pattern(regexp = "^[0-9A-HJ-NPQRTUWXY]{17}[0-9A-HJ-NPQRTUWXY]$", 
             message = "统一社会信用代码格式不正确")
    private String taxId;

    /**
     * 地址
     */
    private String address;

    /**
     * 电话
     */
    private String phone;

    /**
     * 开户银行名称
     */
    private String bankName;

    /**
     * 银行账号
     */
    private String bankAccount;
}
