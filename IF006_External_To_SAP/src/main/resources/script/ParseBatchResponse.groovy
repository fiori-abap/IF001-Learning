import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def Message processData(Message message) {

    def body = message.getBody(String)
    def recordCount = message.getProperty("RecordCount")

    int count = 0
    if (recordCount != null) {
        count = new BigDecimal(recordCount.toString()).intValue()
    }

    // Batch 外层即使是 200，
    // ChangeSet 内部仍可能包含 400 / 500
    def statusMatcher = body =~ /HTTP\/1\.[01]\s+([45]\d\d)\b/

    boolean hasError =
        statusMatcher.find() ||
        body.contains('"error"')

    if (hasError) {

        int httpStatus = 400

        // 如果 multipart 内部明确有 4xx / 5xx，就取它
        def statusMatcher2 = body =~ /HTTP\/1\.[01]\s+([45]\d\d)\b/
        if (statusMatcher2.find()) {
            httpStatus = statusMatcher2.group(1).toInteger()
        }

        def errorCode = ""
        def errorMessage = "Batch request failed"

        // 从 multipart 中找到 SAP 返回的 {"error": ...}
        int errorPos = body.indexOf('"error"')

        if (errorPos >= 0) {

            int jsonStart = body.lastIndexOf('{', errorPos)
            int jsonEnd   = body.lastIndexOf('}')

            if (jsonStart >= 0 && jsonEnd > jsonStart) {

                def jsonText = body.substring(jsonStart, jsonEnd + 1)

                try {
                    def sapError = new JsonSlurper().parseText(jsonText)

                    errorCode =
                        sapError?.error?.code ?: ""

                    errorMessage =
                        sapError?.error?.message ?: errorMessage

                } catch (Exception ignored) {
                    // 解析失败时保留默认错误信息
                }
            }
        }

        def response = [
            status  : "ERROR",
            code    : errorCode,
            message : errorMessage
        ]

        message.setBody(JsonOutput.toJson(response))
        message.setHeader("CamelHttpResponseCode", httpStatus)

    } else {

        def response = [
            status : "SUCCESS",
            count  : count
        ]

        message.setBody(JsonOutput.toJson(response))
        message.setHeader("CamelHttpResponseCode", 200)
    }

    message.setHeader("Content-Type", "application/json")

    return message
}