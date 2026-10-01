import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {

    def body = message.getBody(String)
    def xml  = new XmlSlurper().parseText(body)

    def batchBoundary     = "batch_if006"
    def changeSetBoundary = "changeset_if006"

    def lines = []

    // 外层 Batch 开始
    lines << "--${batchBoundary}"
    lines << "Content-Type: multipart/mixed; boundary=${changeSetBoundary}"
    lines << ""

    int contentId = 1

    xml.ExternalData.each { item ->

        def payload = [
            ExternalOrderNo : item.ExternalOrderNo.text(),
            ExternalItemNo  : item.ExternalItemNo.text(),
            Material        : item.Material.text(),
            Amount          : new BigDecimal(item.Amount.text()),
            Currency        : item.Currency.text(),
            Status          : item.Status.text(),
            SourceSystem    : item.SourceSystem.text()
        ]

        lines << "--${changeSetBoundary}"
        lines << "Content-Type: application/http"
        lines << "Content-Transfer-Encoding: binary"
        lines << "Content-ID: ${contentId}"
        lines << ""
        lines << "POST ExternalData HTTP/1.1"
        lines << "Content-Type: application/json"
        lines << "Accept: application/json"
        lines << ""
        lines << JsonOutput.toJson(payload)

        contentId++
    }

    // ChangeSet 结束
    lines << "--${changeSetBoundary}--"

    // Batch 结束
    lines << "--${batchBoundary}--"
    lines << ""

    // Batch 对换行很敏感，用 CRLF
    message.setBody(lines.join("\r\n"))

    message.setHeader(
        "Content-Type",
        "multipart/mixed; boundary=${batchBoundary}"
    )

    return message
}