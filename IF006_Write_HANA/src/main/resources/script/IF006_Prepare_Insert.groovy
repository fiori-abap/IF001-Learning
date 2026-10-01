import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder

Message processData(Message message) {

    // 1. 接收 SAP JSON
    def data = new JsonSlurper().parseText(
        message.getBody(String)
    )

    String writeMode =
        data.WriteMode?.toString()?.trim()?.toUpperCase()

    message.setProperty("WriteMode", writeMode)

    if (!(writeMode in ['I', 'U'])) {
        throw new IllegalArgumentException(
            "WriteMode は I または U を指定してください"
        )
    }

    // 2. 兼容旧单条和新批量结构
    def sourceOrders

    if (data.containsKey('Orders')) {

        if (!(data.Orders instanceof List)) {
            throw new IllegalArgumentException(
                "Orders は配列で指定してください"
            )
        }

        sourceOrders = data.Orders

    } else {

        // 原来的单条 sendToCloud 仍然能够使用
        sourceOrders = [data]

    }

    if (!sourceOrders || sourceOrders.size() > 100) {
        throw new IllegalArgumentException(
            "送信件数は1件から100件まで指定してください"
        )
    }

    // 3. 先检查全部订单，不通过就不进入 JDBC
    def orders = []
    def keys = new HashSet()

    sourceOrders.eachWithIndex { row, index ->

        if (!(row instanceof Map)) {
            throw new IllegalArgumentException(
                "${index + 1}件目：データ形式が不正です"
            )
        }

        String orderNo =
            row.ExternalOrderNo?.toString()?.trim()

        String itemNo =
            row.ExternalItemNo?.toString()?.trim()

        String material =
            row.Material?.toString()?.trim()

        String currency =
            row.Currency?.toString()?.trim()

        if (!orderNo || !itemNo || !material ||
            !currency || row.Amount == null) {

            throw new IllegalArgumentException(
                "${index + 1}件目：必須項目が未入力です"
            )
        }

        if (orderNo.length() > 20 ||
            itemNo.length() > 10 ||
            material.length() > 40 ||
            currency.length() > 5) {

            throw new IllegalArgumentException(
                "${index + 1}件目：項目長が上限を超えています"
            )
        }

        BigDecimal amount

        try {

            amount = new BigDecimal(
                row.Amount.toString()
            )

        } catch (Exception e) {

            throw new IllegalArgumentException(
                "${index + 1}件目：金額が不正です"
            )

        }

        if (amount.scale() > 2 ||
            amount.abs() > new BigDecimal("9999999999999.99")) {

            throw new IllegalArgumentException(
                "${index + 1}件目：金額が範囲外です"
            )
        }

        // 同一批次内部不允许重复主键
        String key = orderNo + '\u0000' + itemNo

        if (!keys.add(key)) {

            throw new IllegalArgumentException(
                "${index + 1}件目：送信データ内でキーが重複しています"
            )

        }

        // 检查合格后保存转换结果
        orders.add([
            orderNo : orderNo,
            itemNo  : itemNo,
            material: material,
            amount  : amount.toPlainString(),
            currency: currency
        ])

    }

    // 4. 生成一份包含多条语句的 JDBC XML
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    xml.root {

        if (writeMode == 'I') {

            xml.InsertStatement {

                dbTableName(action: 'INSERT') {

                    table("IF006_EXT.EXT_ORDERS")

                    orders.each { order ->

                        access {

                            ORDER_NO(
                                hasQuot: "Yes",
                                order.orderNo
                            )

                            ITEM_NO(
                                hasQuot: "Yes",
                                order.itemNo
                            )

                            PRODUCT_CODE(
                                hasQuot: "Yes",
                                order.material
                            )

                            AMOUNT(order.amount)

                            CURR_CODE(
                                hasQuot: "Yes",
                                order.currency
                            )
                        }
                    }
                }
            }

        } else {

            // U 模式暂时保持原来的写法
            orders.each { order ->

                xml.Statement {

                    dbTableName(action: 'UPDATE_INSERT') {

                        table("IF006_EXT.EXT_ORDERS")

                        access {

                            ORDER_NO(
                                hasQuot: "Yes",
                                order.orderNo
                            )

                            ITEM_NO(
                                hasQuot: "Yes",
                                order.itemNo
                            )

                            PRODUCT_CODE(
                                hasQuot: "Yes",
                                order.material
                            )

                            AMOUNT(order.amount)

                            CURR_CODE(
                                hasQuot: "Yes",
                                order.currency
                            )
                        }

                        key1 {

                            ORDER_NO(
                                hasQuot: "Yes",
                                order.orderNo
                            )

                            ITEM_NO(
                                hasQuot: "Yes",
                                order.itemNo
                            )
                        }
                    }
                }
            }
        }
    }

    message.setBody(writer.toString())

    return message
}