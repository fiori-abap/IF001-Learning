import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def Message processData(Message message) {

    def ex = message.getProperty("CamelExceptionCaught")

    def responseBody = null
    def statusCode = 500

    def current = ex

    while (current != null) {

        if (current.getClass().getCanonicalName() ==
            "org.apache.camel.component.ahc.AhcOperationFailedException") {

            responseBody = current.getResponseBody()
            statusCode   = current.getStatusCode()
            break
        }

        current = current.getCause()
    }

    def errorMessage = "Unknown error"
    def errorCode = ""

    if (responseBody != null && responseBody.trim()) {

        try {
            def sapError = new JsonSlurper().parseText(responseBody)

            errorCode    = sapError?.error?.code ?: ""
            errorMessage = sapError?.error?.message ?: responseBody

        } catch (Exception ignored) {
            errorMessage = responseBody
        }
    }
    else if (ex != null) {
        errorMessage = ex.getMessage()
    }

    def response = [
        status  : "ERROR",
        code    : errorCode,
        message : errorMessage
    ]

    // ① 构造错误响应
    message.setBody(JsonOutput.toJson(response))

    message.setHeader("Content-Type", "application/json")
    message.setHeader("CamelHttpResponseCode", statusCode)

    // ② 清理认证和会话 Header
    def headers = new LinkedHashMap(message.getHeaders())

    def removeNames = [
        "set-cookie",
        "cookie",
        "x-csrf-token",
        "authorization"
    ]

    headers.keySet().removeAll { name ->
        removeNames.contains(name.toString().toLowerCase())
    }

    message.setHeaders(headers)

    return message
}