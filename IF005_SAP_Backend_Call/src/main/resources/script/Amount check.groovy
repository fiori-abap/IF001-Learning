import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def body = message.getBody(String)
    def xml = new XmlSlurper().parseText(body)

    def errors = []

    xml.orders.each { order ->

        def orderNo   = order.externalOrderNo.text()
        def amountTxt = order.grossAmount.text()
        def amount    = new BigDecimal(amountTxt)

        if (amount > 9000) {
            errors.add(
                "Order ${orderNo}: GrossAmount ${amountTxt} exceeds 9000"
            )
        }
    }

    // 全部检查结束以后，一次性抛出所有错误
    if (!errors.isEmpty()) {
        throw new RuntimeException(
            errors.join(" | ")
        )
    }

    return message
}