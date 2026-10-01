import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    // 读取 Message Mapping 返回的 XML
    def xml = new XmlSlurper().parseText(message.getBody(String))

    // 每个 ExternalOrder 转换为一条 JSON 数据
    def orders = xml.ExternalOrder.collect { order ->

        def amountText = order.Amount.text().trim()

        return [
            ExternalOrderNo : order.ExternalOrderNo.text(),
            ExternalItemNo  : order.ExternalItemNo.text(),
            Material        : order.Material.text(),
            Amount          : amountText ? new BigDecimal(amountText) : null,
            Currency        : order.Currency.text()
        ]
    }

    // 组成 Fiori 预览使用的 JSON
    def response = [
        orders: orders
    ]

    message.setBody(JsonOutput.toJson(response))
    message.setHeader("Content-Type", "application/json")

    return message
}