import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def query = message.getHeader("CamelHttpQuery", String)

    def params = [:]

    if (query) {
        query.split('&').each { pair ->
            def parts = pair.split('=', 2)

            if (parts.size() == 2) {
                params[parts[0]] = parts[1]
            }
        }
    }

    message.setProperty("orderNo", params["orderNo"] ?: "")
    message.setProperty("itemNo",  params["itemNo"]  ?: "")

    return message
}