import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def exception = message.getProperty("CamelExceptionCaught")

    def rootCause = exception

    while (rootCause != null && rootCause.getCause() != null) {
        rootCause = rootCause.getCause()
    }

    def errorMessage =
        rootCause?.getMessage() ?: "Unknown integration error"

    message.setBody(errorMessage)

    message.setHeader(
        "CamelHttpResponseCode",
        400
    )

    message.setHeader(
        "Content-Type",
        "text/plain;charset=UTF-8"
    )

    return message
}