#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数字化电子发票生成器
基于《数字化电子发票版式及 XML 规范 V6.001》

支持生成：
- XML: 符合国家税务总局规范的发票数据文件
- OFD: 版式文件（Open Fixed-layout Document）
- PDF: 便携式文档格式
"""

import os
import json
import zipfile
import xml.etree.ElementTree as ET
from xml.dom import minidom
from datetime import datetime
from typing import Dict, List, Optional
import io


class InvoiceTemplate:
    """发票模板类 - 基于 V6.001 规范"""
    
    # 税务局代码映射
    TAX_BUREAU_CODES = {
        "北京市": "1100",
        "天津市": "1200",
        "河北省": "1300",
        "山西省": "1400",
        "内蒙古自治区": "1500",
        "辽宁省": "2100",
        "吉林省": "2200",
        "黑龙江省": "2300",
        "上海市": "3100",
        "江苏省": "3200",
        "浙江省": "3300",
        "安徽省": "3400",
        "福建省": "3500",
        "江西省": "3600",
        "山东省": "3700",
        "河南省": "4100",
        "湖北省": "4200",
        "湖南省": "4300",
        "广东省": "4400",
        "广西壮族自治区": "4500",
        "海南省": "4600",
        "重庆市": "5000",
        "四川省": "5100",
        "贵州省": "5200",
        "云南省": "5300",
        "西藏自治区": "5400",
        "陕西省": "6100",
        "甘肃省": "6200",
        "青海省": "6300",
        "宁夏回族自治区": "6400",
        "新疆维吾尔自治区": "6500",
        "大连市": "2400",
        "青岛市": "3702",
        "宁波市": "3302",
        "厦门市": "3502",
        "深圳市": "4403",
    }
    
    # 发票类型代码
    INVOICE_TYPE_CODES = {
        "增值税专用发票": "01",
        "增值税普通发票": "02",
        "电子发票（普通发票）": "02",
        "电子发票（增值税专用发票）": "01",
    }
    
    def __init__(self):
        self.template_version = "V6.001"
        
    def generate_invoice_number(self, tax_bureau_code: str, date: str = None) -> str:
        """
        生成发票号码（20 位）
        规则：6 位税务局代码 + 2 位年度 + 2 位行业 + 10 位顺序号
        """
        if date is None:
            date = datetime.now().strftime("%Y%m%d")
        
        year = date[2:4]  # 取年份后两位
        industry = "53"  # 软件信息技术服务业
        
        # 生成 10 位随机顺序号
        import random
        serial = ''.join([str(random.randint(0, 9)) for _ in range(10)])
        
        return f"{tax_bureau_code}{year}{industry}{serial}"
    
    def validate_tax_id(self, tax_id: str) -> bool:
        """验证统一社会信用代码格式"""
        if len(tax_id) != 18:
            return False
        # 简单验证：前 17 位为数字或大写字母，最后 1 位为数字或大写字母
        import re
        return bool(re.match(r'^[0-9A-HJ-NPQRTUWXY]{17}[0-9A-HJ-NPQRTUWXY]$', tax_id))


class InvoiceXMLGenerator:
    """发票 XML 生成器 - 符合 V6.001 规范"""
    
    NAMESPACE = "http://www.chinatax.gov.cn/2019/InvoiceML"
    
    def __init__(self, template: InvoiceTemplate):
        self.template = template
    
    def generate(self, invoice_data: Dict) -> str:
        """
        生成发票 XML
        
        Args:
            invoice_data: 发票数据字典，包含：
                - invoice_type: 发票类型
                - seller: 销售方信息
                - buyer: 购买方信息
                - items: 商品明细列表
                - total_amount: 合计金额
                - total_tax: 合计税额
                - invoice_date: 开票日期
                - drawer: 开票人
        
        Returns:
            XML 字符串
        """
        root = ET.Element("Invoice", xmlns=self.NAMESPACE)
        
        # 发票基本信息
        header = ET.SubElement(root, "Header")
        
        # 发票代码和号码
        invoice_code = ET.SubElement(header, "InvoiceCode")
        invoice_code.text = invoice_data.get("invoice_code", "")
        
        invoice_no = ET.SubElement(header, "InvoiceNo")
        invoice_no.text = invoice_data["invoice_no"]
        
        # 开票日期
        issue_date = ET.SubElement(header, "IssueDate")
        issue_date.text = invoice_data["invoice_date"]
        
        # 校验码
        check_code = ET.SubElement(header, "CheckCode")
        check_code.text = invoice_data.get("check_code", "")
        
        # 机器编号
        machine_no = ET.SubElement(header, "MachineNo")
        machine_no.text = invoice_data.get("machine_no", "")
        
        # 销售方信息
        seller_info = ET.SubElement(root, "SellerInfo")
        seller = invoice_data["seller"]
        
        seller_name = ET.SubElement(seller_info, "SellerName")
        seller_name.text = seller["name"]
        
        seller_tax_id = ET.SubElement(seller_info, "SellerTaxID")
        seller_tax_id.text = seller["tax_id"]
        
        seller_addr = ET.SubElement(seller_info, "SellerAddress")
        seller_addr.text = seller.get("address", "")
        
        seller_phone = ET.SubElement(seller_info, "SellerPhone")
        seller_phone.text = seller.get("phone", "")
        
        seller_bank = ET.SubElement(seller_info, "SellerBankName")
        seller_bank.text = seller.get("bank_name", "")
        
        seller_bank_acc = ET.SubElement(seller_info, "SellerBankAccount")
        seller_bank_acc.text = seller.get("bank_account", "")
        
        # 购买方信息
        buyer_info = ET.SubElement(root, "BuyerInfo")
        buyer = invoice_data["buyer"]
        
        buyer_name = ET.SubElement(buyer_info, "BuyerName")
        buyer_name.text = buyer["name"]
        
        buyer_tax_id = ET.SubElement(buyer_info, "BuyerTaxID")
        buyer_tax_id.text = buyer["tax_id"]
        
        buyer_addr = ET.SubElement(buyer_info, "BuyerAddress")
        buyer_addr.text = buyer.get("address", "")
        
        buyer_phone = ET.SubElement(buyer_info, "BuyerPhone")
        buyer_phone.text = buyer.get("phone", "")
        
        buyer_bank = ET.SubElement(buyer_info, "BuyerBankName")
        buyer_bank.text = buyer.get("bank_name", "")
        
        buyer_bank_acc = ET.SubElement(buyer_info, "BuyerBankAccount")
        buyer_bank_acc.text = buyer.get("bank_account", "")
        
        # 商品明细
        items_elem = ET.SubElement(root, "Items")
        for item in invoice_data["items"]:
            item_elem = ET.SubElement(items_elem, "Item")
            
            name = ET.SubElement(item_elem, "ItemName")
            name.text = item["name"]
            
            spec = ET.SubElement(item_elem, "Specification")
            spec.text = item.get("specification", "")
            
            unit = ET.SubElement(item_elem, "Unit")
            unit.text = item.get("unit", "")
            
            quantity = ET.SubElement(item_elem, "Quantity")
            quantity.text = str(item.get("quantity", "1"))
            
            unit_price = ET.SubElement(item_elem, "UnitPrice")
            unit_price.text = str(item["unit_price"])
            
            amount = ET.SubElement(item_elem, "Amount")
            amount.text = str(item["amount"])
            
            tax_rate = ET.SubElement(item_elem, "TaxRate")
            tax_rate.text = str(item.get("tax_rate", "6"))
            
            tax_amount = ET.SubElement(item_elem, "TaxAmount")
            tax_amount.text = str(item.get("tax_amount", "0"))
        
        # 合计信息
        total_info = ET.SubElement(root, "TotalInfo")
        
        total_amount = ET.SubElement(total_info, "TotalAmount")
        total_amount.text = str(invoice_data["total_amount"])
        
        total_tax = ET.SubElement(total_info, "TotalTaxAmount")
        total_tax.text = str(invoice_data["total_tax"])
        
        total_with_tax = ET.SubElement(total_info, "TotalAmountWithTax")
        total_with_tax.text = str(invoice_data["total_amount"] + invoice_data["total_tax"])
        
        # 价税合计（大写）
        total_cn = ET.SubElement(total_info, "TotalAmountWithTaxCN")
        total_cn.text = self._number_to_chinese(invoice_data["total_amount"] + invoice_data["total_tax"])
        
        # 备注
        if invoice_data.get("remarks"):
            remarks = ET.SubElement(root, "Remarks")
            remarks.text = invoice_data["remarks"]
        
        # 开票人
        drawer = ET.SubElement(root, "Drawer")
        drawer.text = invoice_data["drawer"]
        
        # 收款人
        if invoice_data.get("payee"):
            payee = ET.SubElement(root, "Payee")
            payee.text = invoice_data["payee"]
        
        # 复核人
        if invoice_data.get("reviewer"):
            reviewer = ET.SubElement(root, "Reviewer")
            reviewer.text = invoice_data["reviewer"]
        
        # 美化 XML 输出
        xml_str = ET.tostring(root, encoding='utf-8', method='xml')
        dom = minidom.parseString(xml_str)
        return dom.toprettyxml(indent="  ", encoding='utf-8').decode('utf-8')
    
    def _number_to_chinese(self, amount: float) -> str:
        """将数字转换为中文大写金额"""
        chinese_digits = "零壹贰叁肆伍陆柒捌玖"
        units = "元角分"
        
        # 处理整数部分和小数部分
        integer_part = int(amount)
        decimal_part = round((amount - integer_part) * 100)
        
        # 转换整数部分
        if integer_part == 0:
            result = "零元"
        else:
            result = ""
            digits = []
            while integer_part > 0:
                digits.append(integer_part % 10)
                integer_part //= 10
            
            for i, d in enumerate(reversed(digits)):
                result += chinese_digits[d]
                if i < len(digits) - 1:
                    result += "仟佰拾元" [len(digits) - 2 - i]
        
        # 添加小数部分
        jiao = decimal_part // 10
        fen = decimal_part % 10
        
        if jiao > 0 or fen > 0:
            result += chinese_digits[jiao] + "角"
            if fen > 0:
                result += chinese_digits[fen] + "分"
            else:
                result += "整"
        else:
            result += "整"
        
        return result


class InvoiceOFDGenerator:
    """发票 OFD 生成器 - 符合 V6.001 规范"""
    
    OFD_NAMESPACE = "http://www.ofdspec.org/2016"
    
    def __init__(self, template: InvoiceTemplate):
        self.template = template
    
    def generate(self, invoice_data: Dict, output_path: str) -> str:
        """
        生成 OFD 文件
        
        Args:
            invoice_data: 发票数据字典
            output_path: 输出文件路径
        
        Returns:
            生成的 OFD 文件路径
        """
        # 创建 OFD 文件（本质是 ZIP 格式）
        with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as ofd_zip:
            # 1. 生成 OFD.xml（根描述文件）
            ofd_xml = self._generate_ofd_xml(invoice_data)
            ofd_zip.writestr('OFD.xml', ofd_xml)
            
            # 2. 生成 Doc_0/Document.xml
            doc_xml = self._generate_document_xml()
            ofd_zip.writestr('Doc_0/Document.xml', doc_xml)
            
            # 3. 生成公共资源文件
            public_res_xml = self._generate_public_res_xml()
            ofd_zip.writestr('Doc_0/PublicRes_0.xml', public_res_xml)
            
            # 4. 生成文档资源文件
            doc_res_xml = self._generate_document_res_xml()
            ofd_zip.writestr('Doc_0/DocumentRes_0.xml', doc_res_xml)
            
            # 5. 生成页面内容
            content_xml = self._generate_page_content(invoice_data)
            ofd_zip.writestr('Doc_0/Pages/Page_0/Content.xml', content_xml)
            
            # 6. 生成自定义标签（发票数据结构）
            tag_xml = self._generate_custom_tags(invoice_data)
            ofd_zip.writestr('Doc_0/Tags/Tag.xml', tag_xml)
            
            # 7. 生成注释
            annot_xml = self._generate_annotations()
            ofd_zip.writestr('Doc_0/Annots/Annotations.xml', annot_xml)
            
            # 8. 生成页面注释
            page_annot_xml = self._generate_page_annotation()
            ofd_zip.writestr('Doc_0/Annots/Page_0/Annotation.xml', page_annot_xml)
            
            # 9. 生成自定义标签索引
            custom_tags_xml = self._generate_custom_tags_index()
            ofd_zip.writestr('Doc_0/Tags/CustomTags.xml', custom_tags_xml)
        
        return output_path
    
    def _generate_ofd_xml(self, invoice_data: Dict) -> str:
        """生成 OFD.xml 根描述文件"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:OFD xmlns:ofd="{self.OFD_NAMESPACE}" Version="1.1" DocType="OFD">
  <ofd:DocBody>
    <ofd:DocInfo>
      <ofd:DocID>{self._generate_doc_id(invoice_data)}</ofd:DocID>
      <ofd:Creator>e-invoice-generator</ofd:Creator>
      <ofd:CustomDatas>
        <ofd:CustomData Name="发票号码">{invoice_data["invoice_no"]}</ofd:CustomData>
        <ofd:CustomData Name="开票日期">{invoice_data["invoice_date"]}</ofd:CustomData>
        <ofd:CustomData Name="合计金额">{invoice_data["total_amount"]}</ofd:CustomData>
        <ofd:CustomData Name="合计税额">{invoice_data["total_tax"]}</ofd:CustomData>
        <ofd:CustomData Name="价税合计">{invoice_data["total_amount"] + invoice_data["total_tax"]}</ofd:CustomData>
        <ofd:CustomData Name="购买方纳税人识别号">{invoice_data["buyer"]["tax_id"]}</ofd:CustomData>
        <ofd:CustomData Name="销售方纳税人识别号">{invoice_data["seller"]["tax_id"]}</ofd:CustomData>
      </ofd:CustomDatas>
    </ofd:DocInfo>
    <ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot>
  </ofd:DocBody>
</ofd:OFD>'''
        return xml
    
    def _generate_document_xml(self) -> str:
        """生成 Document.xml"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:Document xmlns:ofd="{self.OFD_NAMESPACE}">
  <ofd:CommonData>
    <ofd:MaxUnitID>98</ofd:MaxUnitID>
    <ofd:PageArea>
      <ofd:PhysicalBox>0 0 210 297</ofd:PhysicalBox>
    </ofd:PageArea>
    <ofd:PublicRes>PublicRes_0.xml</ofd:PublicRes>
    <ofd:DocumentRes>DocumentRes_0.xml</ofd:DocumentRes>
  </ofd:CommonData>
  <ofd:Pages>
    <ofd:Page ID="4" BaseLoc="Pages/Page_0/Content.xml"/>
  </ofd:Pages>
  <ofd:Annotations>Annots/Annotations.xml</ofd:Annotations>
  <ofd:CustomTags>Tags/CustomTags.xml</ofd:CustomTags>
</ofd:Document>'''
        return xml
    
    def _generate_public_res_xml(self) -> str:
        """生成公共资源文件"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:Res xmlns:ofd="{self.OFD_NAMESPACE}" BaseLoc="Res">
  <ofd:Fonts>
    <ofd:Font ID="1" FontName="宋体"/>
    <ofd:Font ID="2" FontName="楷体"/>
    <ofd:Font ID="3" FontName="黑体"/>
    <ofd:Font ID="37" FontName="Courier New"/>
  </ofd:Fonts>
</ofd:Res>'''
        return xml
    
    def _generate_document_res_xml(self) -> str:
        """生成文档资源文件"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:Res xmlns:ofd="{self.OFD_NAMESPACE}" BaseLoc="Res">
  <ofd:MultiMedias>
    <ofd:MultiMedia ID="7" Type="Image" Format="png">
      <ofd:MediaFile>Image_0.png</ofd:MediaFile>
    </ofd:MultiMedia>
    <ofd:MultiMedia ID="9" Type="Image" Format="png">
      <ofd:MediaFile>Image_1.png</ofd:MediaFile>
    </ofd:MultiMedia>
    <ofd:MultiMedia ID="11" Type="Image" Format="png">
      <ofd:MediaFile>Image_2.png</ofd:MediaFile>
    </ofd:MultiMedia>
  </ofd:MultiMedias>
  <ofd:DrawParams>
    <ofd:DrawParam ID="13">
      <ofd:FillColor Value="128 0 0"/>
    </ofd:DrawParam>
    <ofd:DrawParam ID="31">
      <ofd:StrokeColor Value="128 0 0"/>
    </ofd:DrawParam>
    <ofd:DrawParam ID="92">
      <ofd:StrokeColor Value="118 89 84"/>
    </ofd:DrawParam>
    <ofd:DrawParam ID="94">
      <ofd:StrokeColor Value="246 237 225"/>
    </ofd:DrawParam>
    <ofd:DrawParam ID="96">
      <ofd:FillColor Value="129 129 129"/>
    </ofd:DrawParam>
  </ofd:DrawParams>
</ofd:Res>'''
        return xml
    
    def _generate_page_content(self, invoice_data: Dict) -> str:
        """生成页面内容 XML - 简化版发票布局"""
        # 这里需要详细的坐标和文本对象定义
        # 实际应用中应该根据 V6.001 规范的版式要求精确布局
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:Page xmlns:ofd="{self.OFD_NAMESPACE}">
  <ofd:Area>
    <ofd:PhysicalBox>0 0 210 140</ofd:PhysicalBox>
  </ofd:Area>
  <ofd:Content>
    <ofd:Layer ID="5">
      <!-- 发票标题 -->
      <ofd:TextObject ID="12" Boundary="56 8 95 7.67" DrawParam="13" Font="2" Size="7.07">
        <ofd:TextCode X="12.15" Y="6.079" DeltaX="g 9 7.07">电子发票（普通发票）</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 发票号码 -->
      <ofd:TextObject ID="14" Boundary="154.5 10.9 20 4" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143" DeltaX="g 4 3.175">发票号码：</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="101" Boundary="165 10.9 50 4" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143">{invoice_data["invoice_no"]}</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 开票日期 -->
      <ofd:TextObject ID="15" Boundary="154.5 16.9 20 4" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143" DeltaX="g 4 3.175">开票日期：</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="102" Boundary="165 16.9 30 4" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143">{invoice_data["invoice_date"]}</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 购买方信息 -->
      <ofd:TextObject ID="22" Boundary="11.3 33.8 10.1 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728" DeltaX="g 2 3.175">名称：</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="103" Boundary="22 33.8 40 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{invoice_data["buyer"]["name"]}</ofd:TextCode>
      </ofd:TextObject>
      
      <ofd:TextObject ID="21" Boundary="11.3 44 43.61 4" CTM="0.89 0 0 1 0 0" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143" DeltaX="g 8 3.175 1.588 g 6 3.175">统一社会信用代码/纳税人识别号：</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="104" Boundary="55 44 40 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143">{invoice_data["buyer"]["tax_id"]}</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 销售方信息 -->
      <ofd:TextObject ID="105" Boundary="11.3 53.8 10.1 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728" DeltaX="g 2 3.175">名称：</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="106" Boundary="22 53.8 40 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{invoice_data["seller"]["name"]}</ofd:TextCode>
      </ofd:TextObject>
      
      <ofd:TextObject ID="107" Boundary="11.3 64 43.61 4" CTM="0.89 0 0 1 0 0" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143" DeltaX="g 8 3.175 1.588 g 6 3.175">统一社会信用代码/纳税人识别号：</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="108" Boundary="55 64 40 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="3.143">{invoice_data["seller"]["tax_id"]}</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 商品明细表头 -->
      <ofd:TextObject ID="110" Boundary="11.3 75 20 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">项目名称</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="111" Boundary="50 75 15 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">金额</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="112" Boundary="75 75 10 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">税率</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="113" Boundary="95 75 15 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">税额</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 商品明细 -->
      {self._generate_items_content(invoice_data["items"])}
      
      <!-- 合计 -->
      <ofd:TextObject ID="120" Boundary="11.3 100 20 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">合计</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="121" Boundary="50 100 20 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{invoice_data["total_amount"]:.2f}</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="122" Boundary="95 100 20 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{invoice_data["total_tax"]:.2f}</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 价税合计 -->
      <ofd:TextObject ID="123" Boundary="11.3 110 40 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">价税合计（大写）</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="124" Boundary="50 110 50 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{self._number_to_chinese(invoice_data["total_amount"] + invoice_data["total_tax"])}</ofd:TextCode>
      </ofd:TextObject>
      
      <ofd:TextObject ID="125" Boundary="11.3 120 20 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">（小写）</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="126" Boundary="35 120 30 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">¥{(invoice_data["total_amount"] + invoice_data["total_tax"]):.2f}</ofd:TextCode>
      </ofd:TextObject>
      
      <!-- 开票人 -->
      <ofd:TextObject ID="127" Boundary="11.3 130 20 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">开票人：</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="128" Boundary="30 130 20 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{invoice_data["drawer"]}</ofd:TextCode>
      </ofd:TextObject>
    </ofd:Layer>
  </ofd:Content>
</ofd:Page>'''
        return xml
    
    def _generate_items_content(self, items: List[Dict]) -> str:
        """生成商品明细内容"""
        content = ""
        y_offset = 80
        for i, item in enumerate(items[:5]):  # 最多显示 5 项
            y = y_offset + i * 5
            content += f'''
      <ofd:TextObject ID="{130+i*2}" Boundary="11.3 {y} 30 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{item["name"]}</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="{131+i*2}" Boundary="50 {y} 15 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{item["amount"]:.2f}</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="{132+i*2}" Boundary="75 {y} 10 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{item.get("tax_rate", 6)}%</ofd:TextCode>
      </ofd:TextObject>
      <ofd:TextObject ID="{133+i*2}" Boundary="95 {y} 15 3.17" DrawParam="13" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.728">{item.get("tax_amount", 0):.2f}</ofd:TextCode>
      </ofd:TextObject>'''
        return content
    
    def _generate_custom_tags(self, invoice_data: Dict) -> str:
        """生成自定义标签（发票结构化数据）"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<EInvoice xmlns:ofd="{self.OFD_NAMESPACE}">
  <Header>
    <EIid>
      <ofd:ObjectData>{invoice_data["invoice_no"]}</ofd:ObjectData>
    </EIid>
    <EInvoiceTag>
      <ofd:ObjectData>SWEI5300</ofd:ObjectData>
    </EInvoiceTag>
    <Version>
      <ofd:ObjectData>0.2</ofd:ObjectData>
    </Version>
    <InherentLabel>
      <InIssuType>
        <LabelCode>
          <ofd:ObjectData>Y</ofd:ObjectData>
        </LabelCode>
        <LabelName>
          <ofd:ObjectData>是否蓝字发票标志</ofd:ObjectData>
        </LabelName>
      </InIssuType>
      <EInvoiceType>
        <LabelCode>
          <ofd:ObjectData>02</ofd:ObjectData>
        </LabelCode>
        <LabelName>
          <ofd:ObjectData>电子发票</ofd:ObjectData>
        </LabelName>
      </EInvoiceType>
      <GeneralOrSpecialVAT>
        <LabelCode>
          <ofd:ObjectData>02</ofd:ObjectData>
        </LabelCode>
        <LabelName>
          <ofd:ObjectData>普通发票</ofd:ObjectData>
        </LabelName>
      </GeneralOrSpecialVAT>
    </InherentLabel>
  </Header>
  <EInvoiceData>
    <SellerInformation>
      <SellerIdNum>
        <ofd:ObjectData>{invoice_data["seller"]["tax_id"]}</ofd:ObjectData>
      </SellerIdNum>
      <SellerName>
        <ofd:ObjectData>{invoice_data["seller"]["name"]}</ofd:ObjectData>
      </SellerName>
      <SellerAddr>
        <ofd:ObjectData>{invoice_data["seller"].get("address", "")}</ofd:ObjectData>
      </SellerAddr>
      <SellerTelNum>
        <ofd:ObjectData>{invoice_data["seller"].get("phone", "")}</ofd:ObjectData>
      </SellerTelNum>
      <SellerBankName>
        <ofd:ObjectData>{invoice_data["seller"].get("bank_name", "")}</ofd:ObjectData>
      </SellerBankName>
      <SellerBankAccNum>
        <ofd:ObjectData>{invoice_data["seller"].get("bank_account", "")}</ofd:ObjectData>
      </SellerBankAccNum>
    </SellerInformation>
    <BuyerInformation>
      <BuyerIdNum>
        <ofd:ObjectData>{invoice_data["buyer"]["tax_id"]}</ofd:ObjectData>
      </BuyerIdNum>
      <BuyerName>
        <ofd:ObjectData>{invoice_data["buyer"]["name"]}</ofd:ObjectData>
      </BuyerName>
      <BuyerAddr>
        <ofd:ObjectData>{invoice_data["buyer"].get("address", "")}</ofd:ObjectData>
      </BuyerAddr>
      <BuyerTelNum>
        <ofd:ObjectData>{invoice_data["buyer"].get("phone", "")}</ofd:ObjectData>
      </BuyerTelNum>
      <BuyerBankName>
        <ofd:ObjectData>{invoice_data["buyer"].get("bank_name", "")}</ofd:ObjectData>
      </BuyerBankName>
      <BuyerBankAccNum>
        <ofd:ObjectData>{invoice_data["buyer"].get("bank_account", "")}</ofd:ObjectData>
      </BuyerBankAccNum>
    </BuyerInformation>
    <BasicInformation>
      <TotalAmWithoutTax>
        <ofd:ObjectData>{invoice_data["total_amount"]}</ofd:ObjectData>
      </TotalAmWithoutTax>
      <TotalTaxAm>
        <ofd:ObjectData>{invoice_data["total_tax"]}</ofd:ObjectData>
      </TotalTaxAm>
      <TotalTax-includedAmountInChinese>
        <ofd:ObjectData>{self._number_to_chinese(invoice_data["total_amount"] + invoice_data["total_tax"])}</ofd:ObjectData>
      </TotalTax-includedAmountInChinese>
      <TotalTax-includedAmount>
        <ofd:ObjectData>{invoice_data["total_amount"] + invoice_data["total_tax"]}</ofd:ObjectData>
      </TotalTax-includedAmount>
      <RequestTime>
        <ofd:ObjectData>{datetime.now().strftime("%Y-%m-%d %H:%M:%S")}</ofd:ObjectData>
      </RequestTime>
      <Drawer>
        <ofd:ObjectData>{invoice_data["drawer"]}</ofd:ObjectData>
      </Drawer>
    </BasicInformation>
  </EInvoiceData>
</EInvoice>'''
        return xml
    
    def _generate_annotations(self) -> str:
        """生成注释索引"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:Annotations xmlns:ofd="{self.OFD_NAMESPACE}">
  <ofd:Page PageID="4">
    <ofd:FileLoc>Page_0/Annotation.xml</ofd:FileLoc>
  </ofd:Page>
</ofd:Annotations>'''
        return xml
    
    def _generate_page_annotation(self) -> str:
        """生成页面注释"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:PageAnnot xmlns:ofd="{self.OFD_NAMESPACE}">
  <ofd:Annot ID="97" Type="Watermark" Creator="" LastModDate="">
    <ofd:Appearance>
      <ofd:TextObject ID="98" Boundary="206.5 29.54 3.31 22.23" CTM="0 1 -1 0 3.308 0" DrawParam="96" Font="2" Size="3.175">
        <ofd:TextCode X="0" Y="2.86" DeltaX="3.175 3.175 3.175 3.175 3.175">电子发票</ofd:TextCode>
      </ofd:TextObject>
    </ofd:Appearance>
  </ofd:Annot>
</ofd:PageAnnot>'''
        return xml
    
    def _generate_custom_tags_index(self) -> str:
        """生成自定义标签索引"""
        xml = f'''<?xml version="1.0" encoding="UTF-8"?>
<ofd:CustomTags xmlns:ofd="{self.OFD_NAMESPACE}">
  <ofd:CustomTag TypeID="">
    <ofd:FileLoc>Tag.xml</ofd:FileLoc>
  </ofd:CustomTag>
</ofd:CustomTags>'''
        return xml
    
    def _generate_doc_id(self, invoice_data: Dict) -> str:
        """生成文档 ID"""
        import hashlib
        content = f"{invoice_data['invoice_no']}{invoice_data['invoice_date']}"
        return hashlib.md5(content.encode('utf-8')).hexdigest()
    
    def _number_to_chinese(self, amount: float) -> str:
        """将数字转换为中文大写金额"""
        chinese_digits = "零壹贰叁肆伍陆柒捌玖"
        
        integer_part = int(amount)
        decimal_part = round((amount - integer_part) * 100)
        
        if integer_part == 0:
            result = "零元"
        else:
            result = ""
            digits = []
            while integer_part > 0:
                digits.append(integer_part % 10)
                integer_part //= 10
            
            unit_map = ["", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿"]
            for i, d in enumerate(digits):
                if d > 0:
                    result = chinese_digits[d] + unit_map[i] + result
                elif result and not result.startswith("零"):
                    result = "零" + result
        
        jiao = decimal_part // 10
        fen = decimal_part % 10
        
        if jiao > 0 or fen > 0:
            result += chinese_digits[jiao] + "角"
            if fen > 0:
                result += chinese_digits[fen] + "分"
        else:
            result += "整"
        
        return result


class InvoicePDFGenerator:
    """发票 PDF 生成器"""
    
    def __init__(self, template: InvoiceTemplate):
        self.template = template
    
    def generate(self, invoice_data: Dict, output_path: str) -> str:
        """
        生成 PDF 发票
        
        注意：完整实现需要使用 reportlab 或其他 PDF 库
        这里提供简化的文本版本作为示例
        """
        try:
            from reportlab.lib.pagesizes import A4
            from reportlab.pdfgen import canvas
            from reportlab.pdfbase import pdfmetrics
            from reportlab.pdfbase.ttfonts import TTFont
            
            # 创建 PDF
            c = canvas.Canvas(output_path, pagesize=A4)
            width, height = A4
            
            # 设置字体（需要中文字体文件）
            # pdfmetrics.registerFont(TTFont('SimSun', '/path/to/simsun.ttf'))
            
            y = height - 50
            
            # 标题
            c.setFont("Helvetica-Bold", 16)
            c.drawCentredString(width / 2, y, "Electronic Invoice (Ordinary)")
            y -= 30
            
            # 发票信息
            c.setFont("Helvetica", 10)
            c.drawString(50, y, f"Invoice No: {invoice_data['invoice_no']}")
            y -= 20
            c.drawString(50, y, f"Issue Date: {invoice_data['invoice_date']}")
            y -= 40
            
            # 购买方信息
            c.setFont("Helvetica-Bold", 10)
            c.drawString(50, y, "Buyer Information:")
            y -= 15
            c.setFont("Helvetica", 10)
            buyer = invoice_data["buyer"]
            c.drawString(70, y, f"Name: {buyer['name']}")
            y -= 15
            c.drawString(70, y, f"Tax ID: {buyer['tax_id']}")
            if buyer.get("address"):
                y -= 15
                c.drawString(70, y, f"Address: {buyer['address']}")
            if buyer.get("phone"):
                y -= 15
                c.drawString(70, y, f"Phone: {buyer['phone']}")
            y -= 30
            
            # 销售方信息
            c.setFont("Helvetica-Bold", 10)
            c.drawString(50, y, "Seller Information:")
            y -= 15
            c.setFont("Helvetica", 10)
            seller = invoice_data["seller"]
            c.drawString(70, y, f"Name: {seller['name']}")
            y -= 15
            c.drawString(70, y, f"Tax ID: {seller['tax_id']}")
            if seller.get("address"):
                y -= 15
                c.drawString(70, y, f"Address: {seller['address']}")
            if seller.get("phone"):
                y -= 15
                c.drawString(70, y, f"Phone: {seller['phone']}")
            y -= 30
            
            # 商品明细
            c.setFont("Helvetica-Bold", 10)
            c.drawString(50, y, "Items:")
            y -= 20
            c.setFont("Helvetica", 9)
            
            # 表头
            c.drawString(60, y, "Item Name")
            c.drawString(200, y, "Amount")
            c.drawString(300, y, "Tax Rate")
            c.drawString(400, y, "Tax Amount")
            y -= 15
            
            # 商品列表
            for item in invoice_data["items"]:
                c.drawString(60, y, item["name"])
                c.drawString(200, y, f"{item['amount']:.2f}")
                c.drawString(300, y, f"{item.get('tax_rate', 6)}%")
                c.drawString(400, y, f"{item.get('tax_amount', 0):.2f}")
                y -= 15
            
            y -= 20
            
            # 合计
            c.setFont("Helvetica-Bold", 10)
            c.drawString(60, y, "Total:")
            c.drawString(200, y, f"{invoice_data['total_amount']:.2f}")
            c.drawString(400, y, f"{invoice_data['total_tax']:.2f}")
            y -= 20
            
            # 价税合计
            total_with_tax = invoice_data["total_amount"] + invoice_data["total_tax"]
            c.drawString(50, y, f"Total Amount with Tax: ¥{total_with_tax:.2f}")
            y -= 30
            
            # 开票人
            c.drawString(50, y, f"Drawer: {invoice_data['drawer']}")
            
            c.save()
            return output_path
            
        except ImportError:
            # 如果没有 reportlab，生成简单的文本文件作为占位
            print("Warning: reportlab not installed, generating text file instead")
            txt_path = output_path.replace('.pdf', '.txt')
            with open(txt_path, 'w', encoding='utf-8') as f:
                f.write(self._generate_text_invoice(invoice_data))
            return txt_path
    
    def _generate_text_invoice(self, invoice_data: Dict) -> str:
        """生成文本格式的发票（当没有 PDF 库时）"""
        lines = [
            "=" * 60,
            "电子发票（普通发票）",
            "=" * 60,
            f"发票号码：{invoice_data['invoice_no']}",
            f"开票日期：{invoice_data['invoice_date']}",
            "",
            "购买方信息:",
            f"  名称：{invoice_data['buyer']['name']}",
            f"  统一社会信用代码/纳税人识别号：{invoice_data['buyer']['tax_id']}",
            "",
            "销售方信息:",
            f"  名称：{invoice_data['seller']['name']}",
            f"  统一社会信用代码/纳税人识别号：{invoice_data['seller']['tax_id']}",
            "",
            "商品明细:",
            "-" * 60,
            f"{'项目名称':<30} {'金额':>10} {'税率':>6} {'税额':>10}",
            "-" * 60,
        ]
        
        for item in invoice_data["items"]:
            name = item["name"]
            amount = f"{item['amount']:.2f}"
            tax_rate = f"{item.get('tax_rate', 6)}%"
            tax_amount = f"{item.get('tax_amount', 0):.2f}"
            lines.append(f"{name:<30} {amount:>10} {tax_rate:>6} {tax_amount:>10}")
        
        lines.extend([
            "-" * 60,
            f"{'合计':<30} {invoice_data['total_amount']:>10.2f} {'':>6} {invoice_data['total_tax']:>10.2f}",
            "",
            f"价税合计（大写）：{self._number_to_chinese(invoice_data['total_amount'] + invoice_data['total_tax'])}",
            f"（小写）：¥{(invoice_data['total_amount'] + invoice_data['total_tax']):.2f}",
            "",
            f"开票人：{invoice_data['drawer']}",
            "=" * 60,
        ])
        
        return "\n".join(lines)
    
    def _number_to_chinese(self, amount: float) -> str:
        """将数字转换为中文大写金额"""
        chinese_digits = "零壹贰叁肆伍陆柒捌玖"
        
        integer_part = int(amount)
        decimal_part = round((amount - integer_part) * 100)
        
        if integer_part == 0:
            result = "零元"
        else:
            result = ""
            digits = []
            while integer_part > 0:
                digits.append(integer_part % 10)
                integer_part //= 10
            
            unit_map = ["", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿"]
            for i, d in enumerate(digits):
                if d > 0:
                    result = chinese_digits[d] + unit_map[i] + result
                elif result and not result.startswith("零"):
                    result = "零" + result
        
        jiao = decimal_part // 10
        fen = decimal_part % 10
        
        if jiao > 0 or fen > 0:
            result += chinese_digits[jiao] + "角"
            if fen > 0:
                result += chinese_digits[fen] + "分"
        else:
            result += "整"
        
        return result


class InvoiceAPI:
    """发票生成 API 接口"""
    
    def __init__(self, output_dir: str = "./output"):
        self.output_dir = output_dir
        os.makedirs(output_dir, exist_ok=True)
        
        self.template = InvoiceTemplate()
        self.xml_generator = InvoiceXMLGenerator(self.template)
        self.ofd_generator = InvoiceOFDGenerator(self.template)
        self.pdf_generator = InvoicePDFGenerator(self.template)
    
    def generate_invoice(self, invoice_data: Dict, formats: List[str] = ['xml', 'ofd', 'pdf']) -> Dict[str, str]:
        """
        生成发票文件
        
        Args:
            invoice_data: 发票数据
            formats: 需要生成的格式列表 ['xml', 'ofd', 'pdf']
        
        Returns:
            生成的文件路径字典
        """
        results = {}
        
        # 生成文件名
        invoice_no = invoice_data["invoice_no"]
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        base_name = f"{invoice_no}_{timestamp}"
        
        # 生成 XML
        if 'xml' in formats:
            xml_content = self.xml_generator.generate(invoice_data)
            xml_path = os.path.join(self.output_dir, f"{base_name}.xml")
            with open(xml_path, 'w', encoding='utf-8') as f:
                f.write(xml_content)
            results['xml'] = xml_path
        
        # 生成 OFD
        if 'ofd' in formats:
            ofd_path = os.path.join(self.output_dir, f"{base_name}.ofd")
            self.ofd_generator.generate(invoice_data, ofd_path)
            results['ofd'] = ofd_path
        
        # 生成 PDF
        if 'pdf' in formats:
            pdf_path = os.path.join(self.output_dir, f"{base_name}.pdf")
            self.pdf_generator.generate(invoice_data, pdf_path)
            results['pdf'] = pdf_path
        
        return results
    
    def download_file(self, file_path: str) -> bytes:
        """读取文件内容用于下载"""
        with open(file_path, 'rb') as f:
            return f.read()


# 示例用法
if __name__ == "__main__":
    # 创建 API 实例
    api = InvoiceAPI(output_dir="./invoices")
    
    # 准备发票数据
    invoice_data = {
        "invoice_code": "053002300111",
        "invoice_no": "26532000000566953411",
        "check_code": "12345678901234567890",
        "machine_no": "123456789012",
        "invoice_date": "2024 年 04 月 23 日",
        "seller": {
            "name": "云南百望云数字科技有限公司",
            "tax_id": "91530102MABX07CH11",
            "address": "云南省昆明市五华区东风西路 156 号环球金融写字楼五楼",
            "phone": "0871-63636403",
            "bank_name": "招商银行股份有限公司昆明联盟路支行",
            "bank_account": "871912253410805"
        },
        "buyer": {
            "name": "大理弘仁堂药业有限责任公司",
            "tax_id": "91532901673642569P",
            "address": "云南省大理白族自治州大理市太和街道嘉士伯大道龙泉 B 组团金都富丽小区西侧",
            "phone": "13320558156",
            "bank_name": "中国建设银行股份有限公司大理南诏支行",
            "bank_account": "53001716038051005334"
        },
        "items": [
            {
                "name": "*软件维护服务*技术服务费",
                "specification": "",
                "unit": "次",
                "quantity": 1,
                "unit_price": 14150.94,
                "amount": 14150.94,
                "tax_rate": 6,
                "tax_amount": 849.06
            }
        ],
        "total_amount": 14150.94,
        "total_tax": 849.06,
        "remarks": "销方开户银行：招商银行股份有限公司昆明联盟路支行; 银行账号:871912253410805",
        "drawer": "汤琼",
        "payee": "",
        "reviewer": ""
    }
    
    # 生成所有格式
    files = api.generate_invoice(invoice_data, formats=['xml', 'ofd', 'pdf'])
    
    print("Generated files:")
    for fmt, path in files.items():
        if os.path.exists(path):
            print(f"  {fmt}: {path}")
            print(f"     Size: {os.path.getsize(path)} bytes")
        else:
            print(f"  {fmt}: Failed to generate")
