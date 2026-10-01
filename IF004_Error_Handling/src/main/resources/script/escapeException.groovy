import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def exception = message.getProperty("CamelExceptionCaught")

    String errorMessage = ""

    if (exception != null) {
        errorMessage = exception.getMessage()
    }

    if (errorMessage == null) {
        errorMessage = "Unknown error"
    }

    // XML Escape
    errorMessage = errorMessage
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    message.setProperty("ErrorMessage", errorMessage)

    return message
}