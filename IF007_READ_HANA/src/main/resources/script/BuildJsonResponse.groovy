import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    def xml = new XmlSlurper(false, false)
        .parseText(message.getBody(String))

    def rows = xml.SelectOrderStatus_response.row.collect {
        [
            ORDER_NO: it.ORDER_NO.text(),
            ITEM_NO: it.ITEM_NO.text(),
            DELIVERY_DATE: it.DELIVERY_DATE.text(),
            EXTERNAL_STATUS: it.EXTERNAL_STATUS.text(),
            COMMENT: it.COMMENT.text()
        ]
    }

    message.setBody(
        JsonOutput.toJson([rows: rows])
    )

    message.setHeader(
        "Content-Type",
        "application/json"
    )

    return message
}