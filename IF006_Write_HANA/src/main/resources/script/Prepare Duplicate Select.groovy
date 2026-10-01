import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder

Message processData(Message message) {

    // 1. 取得最初从 RAP 收到的 JSON
    String originalBody =
        message.getProperty("OriginalPayload")?.toString()

    if (!originalBody) {
        throw new IllegalStateException(
            "OriginalPayload が取得できません"
        )
    }

    def data =
        new JsonSlurper().parseText(originalBody)

    def orders = data.Orders

    if (!(orders instanceof List) || orders.isEmpty()) {
        throw new IllegalArgumentException(
            "送信対象データがありません"
        )
    }

    // 2. 生成一次 SELECT 用 XML
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    xml.root {

        SelectStatement {

            dbTableName(action: "SELECT") {

                table("IF006_EXT.EXT_ORDERS")

                // 要返回的字段
                access {
                    ORDER_NO()
                    ITEM_NO()
                }

                // 每一个 key = 一个订单复合主键
                orders.eachWithIndex { order, index ->

                    "key${index + 1}" {

                        ORDER_NO(
                            hasQuot: "Yes",
                            order.ExternalOrderNo.toString().trim()
                        )

                        ITEM_NO(
                            hasQuot: "Yes",
                            order.ExternalItemNo.toString().trim()
                        )
                    }
                }
            }
        }
    }

    message.setBody(writer.toString())

    return message
}