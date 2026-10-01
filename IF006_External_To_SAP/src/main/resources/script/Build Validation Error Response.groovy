import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def Message processData(Message message) {

    def errorsJson = message.getProperty("ValidationErrors")

    def errors = []

    if (errorsJson != null && errorsJson.toString().trim()) {
        errors = new JsonSlurper().parseText(errorsJson.toString())
    }

    def response = [
        status : "ERROR",
        errors : errors
    ]

    message.setBody(JsonOutput.toJson(response))

    message.setHeader("Content-Type", "application/json")
    message.setHeader("CamelHttpResponseCode", 400)

    return message
}