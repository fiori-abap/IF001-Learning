import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper

Message processData(Message message) {

    // JDBC SELECT 返回结果
    String body = message.getBody(String)

    def xml =
        new XmlSlurper(false, false).parseText(body)

    def rows = xml.depthFirst().findAll { node ->
        node.name().toString() == 'row'
    }

    // --------------------------------------------------
    // 有重复数据：整批停止
    // --------------------------------------------------
    if (!rows.isEmpty()) {

        def duplicateKeys = rows.collect { row ->

            String orderNo =
                row.ORDER_NO.text().trim()

            String itemNo =
                row.ITEM_NO.text().trim()
                    .padLeft(6, '0')

            return "${orderNo} / ${itemNo}"
        }

        throw new IllegalStateException(
            "Cloud登録済み:\n" +
            duplicateKeys.join("\n")
        )
    }

    // --------------------------------------------------
    // 没有重复：恢复最初 JSON，继续真正 INSERT
    // --------------------------------------------------
    String originalPayload =
        message.getProperty("OriginalPayload")?.toString()

    if (!originalPayload) {
        throw new IllegalStateException(
            "OriginalPayload が取得できません"
        )
    }

    message.setBody(originalPayload)

    return message
}