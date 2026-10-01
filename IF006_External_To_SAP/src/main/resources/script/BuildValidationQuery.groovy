import com.sap.gateway.ip.core.customdev.util.Message
import java.net.URLEncoder

def Message processData(Message message) {

    def body = message.getBody(String)
    def xml  = new XmlSlurper().parseText(body)

    def conditions = []

    xml.ExternalData.each { item ->

        def orderNo = item.ExternalOrderNo.text()
        def itemNo  = item.ExternalItemNo.text()

        conditions << "(ExternalOrderNo eq '${orderNo}' and ExternalItemNo eq '${itemNo}')"
    }

    def filter = conditions.join(" or ")

    def encodedFilter =
        URLEncoder.encode(filter, "UTF-8")
                  .replace("+", "%20")

    def query =
        "sap-client=110" +
        "&\$filter=${encodedFilter}" +
        "&\$select=ExternalOrderNo,ExternalItemNo"

    message.setProperty("ValidationQuery", query)

    return message
}