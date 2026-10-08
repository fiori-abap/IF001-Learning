import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import java.util.HashSet

def Message processData(Message message) {

    long t0 = System.nanoTime()

    // ==========================================
    // 1. JSON解析
    // ==========================================
    def request = new JsonSlurper()
        .parseText(message.getBody(String))

    if (!(request.keys instanceof List) ||
        request.keys.isEmpty()) {
        throw new IllegalArgumentException("No order keys")
    }

    long t1 = System.nanoTime()

    // ==========================================
    // 2. HashSet去重
    // ==========================================
    def seen = new HashSet()
    def keys = []

    request.keys.each { key ->

        String orderNo =
            key.orderNo == null ? "" : key.orderNo.toString()

        String itemNo =
            key.itemNo == null ? "" : key.itemNo.toString()

        if (!(orderNo ==~ /[0-9]{10}/) ||
            !(itemNo ==~ /[0-9]{6}/)) {

            throw new IllegalArgumentException(
                "Invalid order key"
            )
        }

        String compositeKey = "${orderNo}|${itemNo}"

        if (seen.add(compositeKey)) {
            keys.add([
                orderNo: orderNo,
                itemNo: itemNo
            ])
        }
    }

    long t2 = System.nanoTime()

    // ==========================================
    // 3. 构建JDBC SELECT XML
    // ==========================================
    def xml = new StringBuilder()

    xml.append('''
<root>
  <SelectOrderStatus>
    <dbTableName action="SELECT">
      <table>IF007_EXT.ORDER_STATUS</table>
      <access>
        <ORDER_NO/>
        <ITEM_NO/>
        <DELIVERY_DATE/>
        <EXTERNAL_STATUS/>
        <COMMENT/>
      </access>
''')

    keys.eachWithIndex { key, index ->

        xml.append("""
      <key${index + 1}>
        <ORDER_NO>${key.orderNo}</ORDER_NO>
        <ITEM_NO>${key.itemNo}</ITEM_NO>
      </key${index + 1}>
""")
    }

    xml.append('''
    </dbTableName>
  </SelectOrderStatus>
</root>
''')

    message.setBody(xml.toString())

    long t3 = System.nanoTime()

    // ==========================================
    // 4. 性能统计
    // ==========================================
    def log = messageLogFactory.getMessageLog(message)

    if (log != null) {

        log.addCustomHeaderProperty(
            "IF007_ParseMs",
            ((t1 - t0) / 1000000.0).toString()
        )

        log.addCustomHeaderProperty(
            "IF007_UniqueMs",
            ((t2 - t1) / 1000000.0).toString()
        )

        log.addCustomHeaderProperty(
            "IF007_XmlMs",
            ((t3 - t2) / 1000000.0).toString()
        )

        log.addCustomHeaderProperty(
            "IF007_TotalMs",
            ((t3 - t0) / 1000000.0).toString()
        )

        log.addCustomHeaderProperty(
            "IF007_KeyCount",
            keys.size().toString()
        )
    }

    return message
}