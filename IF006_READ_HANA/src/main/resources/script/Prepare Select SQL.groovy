import com.sap.gateway.ip.core.customdev.util.Message

Message processData(Message message) {

    String filter =
        message.getProperty("FilterSql")?.toString()?.trim()

    String sql =
        'SELECT * FROM "IF006_EXT"."EXT_ORDERS"'

    if (filter) {

        // 不允许把额外 SQL 语句混进查询条件
        if (filter.contains(';') ||
            filter.contains('--') ||
            filter.contains('/*') ||
            filter.contains('*/')) {

            throw new IllegalArgumentException(
                "Filter 条件が不正です"
            )
        }

        sql += " WHERE " + filter
    }

    message.setBody(sql)

    return message
}