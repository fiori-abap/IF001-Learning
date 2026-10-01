import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {

    def body = message.getBody(String)
    def xml = new XmlSlurper().parseText(body)

    def payload = [
        ExternalOrderNo : xml.ExternalOrderNo.text(),
        ExternalItemNo  : xml.ExternalItemNo.text(),
        Material        : xml.Material.text(),
        Amount          : new BigDecimal(xml.Amount.text()),
        Currency        : xml.Currency.text(),
        Status          : xml.Status.text(),
        SourceSystem    : xml.SourceSystem.text()
    ]

    message.setBody(JsonOutput.toJson(payload))
    message.setHeader("Content-Type", "application/json")

    return message
}