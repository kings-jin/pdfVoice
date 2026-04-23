# 数字化电子发票生成系统 - Spring Boot 实现

基于《数字化电子发票版式及 XML 规范 V6.001》实现的电子发票生成系统，支持生成 XML、OFD、PDF 三种格式的发票文件。

## 技术栈

- **Java 17**
- **Spring Boot 3.2.5**
- **iText 7** (PDF 生成)
- **Lombok**
- **Maven**

## 项目结构

```
e-invoice-spring-boot/
├── src/main/java/com/example/einvoice/
│   ├── EInvoiceApplication.java      # 主启动类
│   ├── config/
│   │   └── WebConfig.java            # Web 配置（CORS 等）
│   ├── controller/
│   │   └── InvoiceController.java    # REST API 控制器
│   ├── generator/
│   │   ├── InvoiceXMLGenerator.java  # XML 生成器
│   │   ├── InvoiceOFDGenerator.java  # OFD 生成器
│   │   └── InvoicePDFGenerator.java  # PDF 生成器
│   ├── model/
│   │   ├── InvoiceData.java          # 发票数据模型
│   │   ├── InvoiceItem.java          # 商品明细模型
│   │   ├── PartyInfo.java            # 交易方信息模型
│   │   └── InvoiceGenerationResult.java # 生成结果模型
│   └── service/
│       └── InvoiceService.java       # 发票生成服务
├── src/main/resources/
│   └── application.yml               # 应用配置
└── pom.xml                           # Maven 配置
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+

### 2. 构建项目

```bash
cd e-invoice-spring-boot
mvn clean package -DskipTests
```

### 3. 运行应用

```bash
java -jar target/e-invoice-1.0.0.jar
```

或使用 Maven:

```bash
mvn spring-boot:run
```

应用启动后访问：http://localhost:8080

## API 接口文档

### 1. 生成发票

**接口**: `POST /api/invoices/generate`

**请求参数**:
- `formats` (可选): 需要生成的格式，逗号分隔 (xml,ofd,pdf)，默认全部生成

**请求体示例**:

```json
{
  "invoiceCode": "053002300111",
  "invoiceNo": "26532000000566953411",
  "checkCode": "12345678901234567890",
  "machineNo": "123456789012",
  "invoiceDate": "2024 年 04 月 23 日",
  "seller": {
    "name": "云南百望云数字科技有限公司",
    "taxId": "91530102MABX07CH11",
    "address": "云南省昆明市五华区东风西路 156 号",
    "phone": "0871-63636403",
    "bankName": "招商银行股份有限公司昆明联盟路支行",
    "bankAccount": "871912253410805"
  },
  "buyer": {
    "name": "大理弘仁堂药业有限责任公司",
    "taxId": "91532901673642569P",
    "address": "云南省大理白族自治州大理市太和街道",
    "phone": "13320558156",
    "bankName": "中国建设银行股份有限公司大理南诏支行",
    "bankAccount": "53001716038051005334"
  },
  "items": [
    {
      "name": "*软件维护服务*技术服务费",
      "specification": "",
      "unit": "次",
      "quantity": 1,
      "unitPrice": 14150.94,
      "amount": 14150.94,
      "taxRate": 6,
      "taxAmount": 849.06
    }
  ],
  "totalAmount": 14150.94,
  "totalTax": 849.06,
  "remarks": "销方开户银行：招商银行股份有限公司昆明联盟路支行",
  "drawer": "汤琼",
  "payee": "",
  "reviewer": ""
}
```

**响应示例**:

```json
{
  "success": true,
  "invoiceNo": "26532000000566953411",
  "xmlPath": "invoices/26532000000566953411_20240423152703.xml",
  "ofdPath": "invoices/26532000000566953411_20240423152703.ofd",
  "pdfPath": "invoices/26532000000566953411_20240423152703.pdf",
  "timestamp": "20240423152703"
}
```

### 2. 下载 XML 文件

**接口**: `GET /api/invoices/download/xml?path={file_path}`

**示例**:
```bash
curl -O http://localhost:8080/api/invoices/download/xml?path=invoices/26532000000566953411_20240423152703.xml
```

### 3. 下载 OFD 文件

**接口**: `GET /api/invoices/download/ofd?path={file_path}`

**示例**:
```bash
curl -O http://localhost:8080/api/invoices/download/ofd?path=invoices/26532000000566953411_20240423152703.ofd
```

### 4. 下载 PDF 文件

**接口**: `GET /api/invoices/download/pdf?path={file_path}`

**示例**:
```bash
curl -O http://localhost:8080/api/invoices/download/pdf?path=invoices/26532000000566953411_20240423152703.pdf
```

### 5. 健康检查

**接口**: `GET /api/invoices/health`

**响应**:
```json
{
  "status": "UP",
  "service": "e-invoice",
  "version": "1.0.0"
}
```

## 使用示例

### cURL 示例

```bash
# 生成发票
curl -X POST http://localhost:8080/api/invoices/generate \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNo": "26532000000566953411",
    "invoiceDate": "2024 年 04 月 23 日",
    "seller": {
      "name": "云南百望云数字科技有限公司",
      "taxId": "91530102MABX07CH11"
    },
    "buyer": {
      "name": "大理弘仁堂药业有限责任公司",
      "taxId": "91532901673642569P"
    },
    "items": [{
      "name": "*软件维护服务*技术服务费",
      "quantity": 1,
      "unitPrice": 14150.94,
      "amount": 14150.94,
      "taxRate": 6,
      "taxAmount": 849.06
    }],
    "totalAmount": 14150.94,
    "totalTax": 849.06,
    "drawer": "汤琼"
  }'

# 只生成 PDF
curl -X POST "http://localhost:8080/api/invoices/generate?formats=pdf" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### Java 调用示例

```java
RestTemplate restTemplate = new RestTemplate();

// 准备发票数据
InvoiceData invoiceData = new InvoiceData();
invoiceData.setInvoiceNo("26532000000566953411");
invoiceData.setInvoiceDate("2024 年 04 月 23 日");
// ... 设置其他字段

// 生成发票
ResponseEntity<InvoiceGenerationResult> response = restTemplate.postForEntity(
    "http://localhost:8080/api/invoices/generate",
    invoiceData,
    InvoiceGenerationResult.class
);

if (response.getBody().isSuccess()) {
    // 下载 PDF
    byte[] pdfBytes = restTemplate.getForObject(
        "http://localhost:8080/api/invoices/download/pdf?path=" + 
        response.getBody().getPdfPath(),
        byte[].class
    );
    
    // 保存文件
    Files.write(Paths.get("invoice.pdf"), pdfBytes);
}
```

## 数据模型说明

### InvoiceData (发票数据)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| invoiceCode | String | 否 | 发票代码 (12 位) |
| invoiceNo | String | 是 | 发票号码 (20 位) |
| checkCode | String | 否 | 校验码 (20 位) |
| machineNo | String | 否 | 机器编号 (12 位) |
| invoiceDate | String | 是 | 开票日期 |
| seller | PartyInfo | 是 | 销售方信息 |
| buyer | PartyInfo | 是 | 购买方信息 |
| items | List<InvoiceItem> | 是 | 商品明细列表 |
| totalAmount | BigDecimal | 是 | 合计金额 (不含税) |
| totalTax | BigDecimal | 是 | 合计税额 |
| remarks | String | 否 | 备注 |
| drawer | String | 是 | 开票人 |
| payee | String | 否 | 收款人 |
| reviewer | String | 否 | 复核人 |

### PartyInfo (交易方信息)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 名称 |
| taxId | String | 是 | 统一社会信用代码 (18 位) |
| address | String | 否 | 地址 |
| phone | String | 否 | 电话 |
| bankName | String | 否 | 开户银行名称 |
| bankAccount | String | 否 | 银行账号 |

### InvoiceItem (商品明细)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 项目名称 |
| specification | String | 否 | 规格型号 |
| unit | String | 否 | 单位 |
| quantity | Integer | 是 | 数量 |
| unitPrice | BigDecimal | 是 | 单价 (不含税) |
| amount | BigDecimal | 是 | 金额 (不含税) |
| taxRate | BigDecimal | 是 | 税率 (%) |
| taxAmount | BigDecimal | 是 | 税额 |

## 注意事项

1. **OFD 格式**: 当前实现提供简化的 OFD 文件生成，完整的 OFD 生成建议使用专业库（如数科 OFD SDK）

2. **PDF 中文字体**: 如需完美支持中文 PDF，需要添加中文字体文件到项目中

3. **文件存储**: 当前实现使用内存存储，生产环境建议：
   - 使用文件系统持久化
   - 或使用对象存储（如阿里云 OSS、AWS S3）
   - 实现文件过期清理策略

4. **安全性**: 生产环境应添加：
   - 身份认证和授权
   - 请求限流
   - 输入验证增强
   - HTTPS 加密传输

## 许可证

本项目仅供学习和参考使用。
