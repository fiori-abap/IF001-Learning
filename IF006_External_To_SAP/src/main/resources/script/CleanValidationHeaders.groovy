import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def headers = new LinkedHashMap(message.getHeaders())

    def removeNames = [
        "set-cookie",
        "cookie",
        "x-csrf-token",
        "authorization"
    ]

    headers.keySet().removeAll { name ->
        removeNames.contains(name.toLowerCase())
    }

    message.setHeaders(headers)

    return message
}