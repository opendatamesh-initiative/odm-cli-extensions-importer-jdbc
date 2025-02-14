package org.opendatamesh.cli.extensions.importerjdbc;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class SQLToJsonSchemaMapper {

    private static final Map<Integer, String> SQL_TO_JSON_SCHEMA = new HashMap<>();

    static {
        SQL_TO_JSON_SCHEMA.put(Types.VARCHAR, "string");
        SQL_TO_JSON_SCHEMA.put(Types.CHAR, "string");
        SQL_TO_JSON_SCHEMA.put(Types.LONGVARCHAR, "string");
        SQL_TO_JSON_SCHEMA.put(Types.NVARCHAR, "string");
        SQL_TO_JSON_SCHEMA.put(Types.NCHAR, "string");
        SQL_TO_JSON_SCHEMA.put(Types.LONGNVARCHAR, "string");
        
        SQL_TO_JSON_SCHEMA.put(Types.INTEGER, "integer");
        SQL_TO_JSON_SCHEMA.put(Types.SMALLINT, "integer");
        SQL_TO_JSON_SCHEMA.put(Types.TINYINT, "integer");
        SQL_TO_JSON_SCHEMA.put(Types.BIGINT, "integer");

        SQL_TO_JSON_SCHEMA.put(Types.FLOAT, "number");
        SQL_TO_JSON_SCHEMA.put(Types.REAL, "number");
        SQL_TO_JSON_SCHEMA.put(Types.DOUBLE, "number");
        SQL_TO_JSON_SCHEMA.put(Types.NUMERIC, "number");
        SQL_TO_JSON_SCHEMA.put(Types.DECIMAL, "number");

        SQL_TO_JSON_SCHEMA.put(Types.BOOLEAN, "boolean");
        SQL_TO_JSON_SCHEMA.put(Types.BIT, "boolean");

        SQL_TO_JSON_SCHEMA.put(Types.DATE, "string");
        SQL_TO_JSON_SCHEMA.put(Types.TIME, "string");
        SQL_TO_JSON_SCHEMA.put(Types.TIMESTAMP, "string");

        SQL_TO_JSON_SCHEMA.put(Types.BLOB, "string");
        SQL_TO_JSON_SCHEMA.put(Types.CLOB, "string");
        SQL_TO_JSON_SCHEMA.put(Types.BINARY, "string");
        SQL_TO_JSON_SCHEMA.put(Types.VARBINARY, "string");
        SQL_TO_JSON_SCHEMA.put(Types.LONGVARBINARY, "string");

        // Default mapping for unhandled types
        SQL_TO_JSON_SCHEMA.put(Types.OTHER, "string");
    }

    public static String mapSqlTypeToJsonSchema(int sqlType) {
        return SQL_TO_JSON_SCHEMA.getOrDefault(sqlType, "string"); // Default to string if unknown
    }
}
