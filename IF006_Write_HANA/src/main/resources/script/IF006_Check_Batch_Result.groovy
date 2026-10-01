import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

Message processData(Message message) {

    // 1. 取得 JDBC Batch 返回的 XML
    String body = message.getBody(String)

    def xml = new XmlSlurper(false, false).parseText(body)

    // 2. 取得前面保存的写入模式
    String writeMode =
        message.getProperty("WriteMode")?.toString()

    int processedCount = 0
    int successCount = 0

    // 3. INSERT 模式
    if (writeMode == 'I') {

        def insertCounts = xml.depthFirst().findAll { node ->
            node.name().toString() == 'insert_count'
        }

        if (insertCounts.isEmpty()) {
            throw new IllegalStateException(
                'JDBC INSERT 実行結果が取得できません'
            )
        }

        insertCounts.each { node ->

            String countText = node.text().trim()

            if (!(countText ==~ /[0-9]+/)) {
                throw new IllegalStateException(
                    'JDBC INSERT 件数が不正です'
                )
            }

            int count = countText.toInteger()

            processedCount += count
            successCount += count
        }

    // 4. UPDATE_INSERT 模式
    } else if (writeMode == 'U') {

        def resultCounts = xml.depthFirst().findAll { node ->

            String name = node.name().toString()

            name == 'update_insert_count' ||
            name == 'update_count' ||
            name == 'insert_count'
        }

        if (resultCounts.isEmpty()) {
            throw new IllegalStateException(
                'JDBC UPDATE_INSERT 実行結果が取得できません'
            )
        }

        resultCounts.each { node ->

            String countText = node.text().trim()

            if (!(countText ==~ /[0-9]+/)) {
                throw new IllegalStateException(
                    'JDBC UPDATE_INSERT 件数が不正です'
                )
            }

            int count = countText.toInteger()

            processedCount += count
            successCount += count
        }

    } else {

        throw new IllegalStateException(
            'WriteMode が不正です'
        )
    }

    // 5. RAP に返す JSON
    def response = [
        ProcessedCount: processedCount,
        SuccessCount  : successCount
    ]

    message.setBody(
        JsonOutput.toJson(response)
    )

    message.setHeader(
        'Content-Type',
        'application/json'
    )

    return message
}