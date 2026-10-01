import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

Message processData(Message message) {

    // 保存最初从 RAP 收到的 JSON
    String originalBody = message.getBody(String)

    def data = new JsonSlurper().parseText(originalBody)

    String writeMode =
        data.WriteMode?.toString()?.trim()?.toUpperCase()

    // 后面 JDBC SELECT 会覆盖 Body，所以先把原始 JSON 保存起来
    message.setProperty(
        "OriginalPayload",
        originalBody
    )

    // 后面 Router 判断 I / U 时使用
    message.setProperty(
        "WriteMode",
        writeMode
    )

    return message
}