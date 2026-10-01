import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {

    def recordCount = message.getProperty("RecordCount")

    def count = 0
    if (recordCount != null) {
        count = new BigDecimal(recordCount.toString()).intValue()
    }

    def response = [
        status : "SUCCESS",
        count  : count
    ]

    message.setBody(JsonOutput.toJson(response))
    message.setHeader("Content-Type", "application/json")

    return message
}