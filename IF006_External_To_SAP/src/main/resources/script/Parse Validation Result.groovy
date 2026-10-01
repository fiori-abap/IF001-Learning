import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    def body = message.getBody(String)
    def result = new JsonSlurper().parseText(body)

    def errors = []

    result.value.each { item ->
        errors << [
            ExternalOrderNo : item.ExternalOrderNo,
            ExternalItemNo  : item.ExternalItemNo,
            message         : "このキー値はすでに使用されています"
        ]
    }

    message.setProperty(
        "ValidationHasError",
        errors.size() > 0 ? "true" : "false"
    )

    message.setProperty(
        "ValidationErrors",
        JsonOutput.toJson(errors)
    )

    return message
}