package dev.star122o.tethersCore

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement

/**
 * Handles SQLite access for the Tethers plugin database.
 */
class DatabaseManager(
    private val databasePath: String = "./plugins/Tethers/data.db",
) {
    /**
     * Opens a new SQLite connection to the configured database file.
     *
     * @return open JDBC connection
     */
    fun connect(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:$databasePath")
    }

    /**
     * Executes a query and returns every row as a map keyed by column label.
     *
     * @param sql SQL query to execute
     * @param params positional parameters bound to the prepared statement
     * @return list of rows
     */
    fun get(sql: String, params: List<Any?> = emptyList()): List<Map<String, Any?>> {
        connect().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                bindParams(statement, params)
                statement.executeQuery().use { resultSet ->
                    val metadata = resultSet.metaData
                    val columnCount = metadata.columnCount
                    val rows = mutableListOf<Map<String, Any?>>()
                    while (resultSet.next()) {
                        val row = mutableMapOf<String, Any?>()
                        for (i in 1..columnCount) {
                            row[metadata.getColumnLabel(i)] = resultSet.getObject(i)
                        }
                        rows.add(row)
                    }
                    return rows
                }
            }
        }
    }

    /**
     * Executes an insert, update, or delete statement.
     *
     * @param sql SQL statement to execute
     * @param params positional parameters bound to the prepared statement
     * @return number of affected rows
     */
    fun set(sql: String, params: List<Any?> = emptyList()): Int {
        connect().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                bindParams(statement, params)
                return statement.executeUpdate()
            }
        }
    }

    /**
     * Executes raw SQL using a plain JDBC statement.
     *
     * @param sql SQL to execute
     * @return true when the execution returns a result set, false otherwise
     */
    fun raw(sql: String): Boolean {
        connect().use { connection ->
            connection.createStatement().use { statement: Statement ->
                return statement.execute(sql)
            }
        }
    }

    private fun bindParams(statement: PreparedStatement, params: List<Any?>) {
        params.forEachIndexed { index, value ->
            statement.setObject(index + 1, value)
        }
    }
}
