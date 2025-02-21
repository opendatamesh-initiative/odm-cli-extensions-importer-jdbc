package org.opendatamesh.cli.extensions.importerjdbc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataStoreApiMerger {


    static JsonNode mergeDataStoreApi(ObjectNode existingDefinitionInput, ObjectNode newDefinition) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Make a deep copy of the existing existingDefinition to avoid modifying the original
        JsonNode existingDefinition = existingDefinitionInput.deepCopy();

        // Extract 'schema' from both the existing and new definitions
        JsonNode existingSchema = existingDefinition.path("schema");
        JsonNode newSchema = newDefinition.path("schema");

        // Get tables from both the existing and new schemas
        JsonNode existingTables = existingSchema.path("tables");
        JsonNode newTables = newSchema.path("tables");

        // Create a list of table names from the new schema for comparison
        List<String> newTableNames = new ArrayList<>();
        newTables.forEach(table -> newTableNames.add(table.path("definition").path("name").asText()));

        // Remove tables from the existing schema that are not present in the new schema
        removeOldTablesNotInNewSchema(existingTables, newTableNames);

        // Merge the tables: update existing tables and add new ones
        mergeTables(existingTables, newTables);

        return existingDefinition;
    }

    /**
     * Removes tables from the existing schema that are not present in the new schema.
     */
    private static void removeOldTablesNotInNewSchema(JsonNode existingTables, List<String> newTableNames) {
        Iterator<JsonNode> existingTableIterator = existingTables.elements();
        while (existingTableIterator.hasNext()) {
            JsonNode existingTable = existingTableIterator.next();
            String existingTableName = existingTable.path("definition").path("name").asText();
            if (!newTableNames.contains(existingTableName)) {
                existingTableIterator.remove(); // Remove the table if it is not in the new schema
            }
        }
    }

    /**
     * Merges the tables: updates existing tables and adds new ones.
     */
    private static void mergeTables(JsonNode existingTables, JsonNode newTables) {
        for (JsonNode newTable : newTables) {
            String newTableName = newTable.path("definition").path("name").asText();

            // Check if the table already exists in the existing schema
            JsonNode existingTable = findTableByName(existingTables, newTableName);
            if (existingTable != null) {
                // If it exists, merge the columns and preserve other properties
                mergeColumns(existingTable, newTable);
                mergeTableProperties(existingTable,newTable);
            } else {
                // If the table doesn't exist, add it to the schema
                ((ArrayNode) existingTables).add(newTable);
            }
        }
    }

    /**
     * Finds a table by its name in the 'tables' array of the schema.
     */
    private static JsonNode findTableByName(JsonNode tables, String tableName) {
        if (tables.isArray()) {
            for (JsonNode table : tables) {
                if (table.path("definition").path("name").asText().equalsIgnoreCase(tableName)) {
                    return table;
                }
            }
        }
        return null;
    }

    /**
     * Merges columns of a table, preserving existing data and adding new columns.
     */
    private static void mergeColumns(JsonNode existingTable, JsonNode newTable) {
        JsonNode existingColumns = existingTable.path("definition").path("properties");
        JsonNode newColumns = newTable.path("definition").path("properties");

        // Create a list of column names in the new schema for comparison
        List<String> newColumnNames = new ArrayList<>();
        newColumns.fieldNames().forEachRemaining(newColumnNames::add);

        // Remove columns that are in the existing schema but not in the new schema
        removeOldColumnsNotInNewSchema(existingColumns, newColumnNames);

        // Merge new columns into the existing ones
        addOrUpdateColumns(existingColumns, newColumns);
    }

    /**
     * Removes columns from the existing schema that are not present in the new schema.
     */
    private static void removeOldColumnsNotInNewSchema(JsonNode existingColumns, List<String> newColumnNames) {
        Iterator<Map.Entry<String, JsonNode>> existingColumnIterator = existingColumns.fields();
        while (existingColumnIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = existingColumnIterator.next();
            String existingColumnName = entry.getKey();
            if (!newColumnNames.contains(existingColumnName)) {
                existingColumnIterator.remove(); // Remove the column if it's not in the new schema
            }
        }
    }

    /**
     * Adds or updates columns in the existing schema based on the new schema.
     */
    private static void addOrUpdateColumns(JsonNode existingColumns, JsonNode newColumns) {
        newColumns.fieldNames().forEachRemaining(columnName -> {
            JsonNode newColumn = newColumns.path(columnName);
            JsonNode existingColumn = existingColumns.path(columnName);

            if (existingColumn.isMissingNode()) {
                // If the column doesn't exist, add it
                ((ObjectNode) existingColumns).set(columnName, newColumn);
            } else {
                // If the column exists, merge properties and preserve existing data
                mergeColumnProperties(existingColumn, newColumn);
            }
        });
    }

    private static void mergeTableProperties(JsonNode existingTable, JsonNode newTable) {
        // Preserve the column properties by ignoring 'properties' field
        ObjectNode existingTableDefinition = (ObjectNode) existingTable.path("definition");
        JsonNode newTableDefinition = newTable.path("definition");

        // Merge the table properties by copying fields from the new table to the existing table
        newTableDefinition.fieldNames().forEachRemaining(fieldName -> {
            if (!fieldName.equals("properties")) {  // Skip the 'properties' field (columns)
                JsonNode newFieldValue = newTableDefinition.path(fieldName);
                existingTableDefinition.set(fieldName, newFieldValue); // Add or overwrite field values
            }
        });
    }

    /**
     * Merges properties of an existing column with a new column, preserving existing properties.
     */
    private static void mergeColumnProperties(JsonNode existingColumn, JsonNode newColumn) {
        ObjectNode mergedColumn = (ObjectNode) existingColumn; // Preserve the existing column data

        newColumn.fieldNames().forEachRemaining(fieldName -> {
            JsonNode newFieldValue = newColumn.path(fieldName);
            mergedColumn.set(fieldName, newFieldValue); // Overwrite or add new field values
        });

    }
}
