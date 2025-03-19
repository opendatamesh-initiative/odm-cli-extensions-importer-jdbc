package org.opendatamesh.cli.extensions.importerjdbc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendatamesh.cli.extensions.ExtensionInfo;
import org.opendatamesh.cli.extensions.ExtensionOption;
import org.opendatamesh.cli.extensions.OdmCliBaseConfiguration;
import org.opendatamesh.cli.extensions.importer.ImporterArguments;
import org.opendatamesh.cli.extensions.importer.ImporterExtension;
import org.opendatamesh.cli.extensions.importerjdbc.datastoreapi.*;
import org.opendatamesh.dpds.model.core.ComponentBase;
import org.opendatamesh.dpds.model.core.StandardDefinition;
import org.opendatamesh.dpds.model.interfaces.Port;
import org.opendatamesh.dpds.model.interfaces.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.opendatamesh.cli.extensions.importerjdbc.DataStoreApiMerger.mergeDataStoreApi;

public class ImporterJDBCExtension implements ImporterExtension<Port> {

    private static final String PARAM_TABLE_TYPES = "--tableTypes";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SUPPORTED_FROM = "jdbc";
    private static final String SUPPORTED_TO = "output-port";

    private static final String PARAM_CONNECTION_NAME = "--connectionName";
    private static final String PARAM_PORT_VERSION = "--portVersion";
    private static final String PARAM_PLATFORM = "--platform";
    private static final String PARAM_CATALOG_NAME = "--catalogName";
    private static final String PARAM_SCHEMA_NAME = "--schemaName";
    private static final String PARAM_TABLES_REGEX = "--tablesPattern";

    private final Map<String, String> parameters = new HashMap<>();

    @Override
    public boolean supports(String from, String to) {
        return SUPPORTED_FROM.equalsIgnoreCase(from) && SUPPORTED_TO.equalsIgnoreCase(to);
    }

    @Override
    public Class<Port> getTargetClass() {
        return Port.class;
    }

    @Override
    public Port importElement(Port targetObject, ImporterArguments importerArguments) {

        validateRequiredParameters();

        // Retrieve connection configuration
        OdmCliBaseConfiguration.SystemConfig connection = importerArguments.getOdmCliConfig()
                .getSystems()
                .stream()
                .filter(s -> importerArguments.getParentCommandOptions().get("source").equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Connection %s not found in ODM config", parameters.get(PARAM_CONNECTION_NAME))
                ));

        String jdbcUrl = connection.getEndpoint();
        String jdbcUser = connection.getUser();
        String jdbcPassword = connection.getPassword();

        // Data structure to hold schema metadata
        DataStoreApiDefinition dataStoreApiDefinition = new DataStoreApiDefinition();
        dataStoreApiDefinition.setDatastoreapi("1.0.0");
        DataStoreApiSchemaResource dataStoreApiSchemaResource = new DataStoreApiSchemaResource();
        dataStoreApiSchemaResource.setTables(new ArrayList<>());
        dataStoreApiSchemaResource.setDatabaseSchemaName(parameters.get(PARAM_SCHEMA_NAME));
        dataStoreApiDefinition.setSchema(dataStoreApiSchemaResource);

        // Retrieve metadata from JDBC
        logger.info("Opening connection to {}", jdbcUrl);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalogName = parameters.get(PARAM_CATALOG_NAME);
            String schemaName = parameters.get(PARAM_SCHEMA_NAME);
            String tablePattern = parameters.get(PARAM_TABLES_REGEX);
            String[] tableTypes = parameters.get(PARAM_TABLE_TYPES) != null ? parameters.get(PARAM_TABLE_TYPES).split(",") : null;

            logger.info("Loading metadata: catalog={}, schema={}, tables={}", catalogName, schemaName, tablePattern);
            try (ResultSet tables = metaData.getTables(catalogName, schemaName, tablePattern, tableTypes)) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String tableSchema = tables.getString("TABLE_SCHEM");
                    String tableCatalog = tables.getString("TABLE_CAT");

                    DataStoreApiSchemaEntity entity = new DataStoreApiSchemaEntity();
                    entity.setSpecification("json-schema");
                    entity.setSpecificationVersion("1");
                    DataStoreAPISchemaEntityDefinition entityJsonSchema = new DataStoreAPISchemaEntityDefinition();
                    entity.setDefinition(entityJsonSchema);

                    entityJsonSchema.setTitle(tableName);
                    entityJsonSchema.setDescription(tables.getString("REMARKS"));
                    entityJsonSchema.setName(tableName);
                    entityJsonSchema.setProperties(new HashMap<>());

                    dataStoreApiSchemaResource.getTables().add(entity);

                    logger.info("Table: {} - {}.{}", tableCatalog, tableSchema, tableName);

                    try (ResultSet columns = metaData.getColumns(tableCatalog, tableSchema, tableName, null)) {
                        while (columns.next()) {
                            DataStoreApiSchemaColumn columnMetadata = new DataStoreApiSchemaColumn();
                            columnMetadata.setName(columns.getString("COLUMN_NAME"));
                            columnMetadata.setType(SQLToJsonSchemaMapper.mapSqlTypeToJsonSchema(columns.getInt("DATA_TYPE")));
                            columnMetadata.setPhysicalType(columns.getString("TYPE_NAME"));
                            columnMetadata.setDescription(columns.getString("REMARKS"));
                            String isNullableISOString = columns.getString("IS_NULLABLE");
                            Boolean isNullable = "YES".equalsIgnoreCase(isNullableISOString) ? Boolean.TRUE
                                    : "NO".equalsIgnoreCase(isNullableISOString) ? Boolean.FALSE
                                    : null;
                            columnMetadata.setNullable(isNullable);
                            columnMetadata.setOrdinalPosition(columns.getString("ORDINAL_POSITION"));
                            entityJsonSchema.getProperties().put(columnMetadata.getName(), columnMetadata);
                            logger.info("--> Column: {}", columnMetadata.getName());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving JDBC metadata", e);
        }

        validateUniqueTableNames(dataStoreApiSchemaResource);
        logger.info("Import completed. Found {} tables.", dataStoreApiSchemaResource.getTables().size());

        Port port = targetObject != null ? targetObject : new Port();
        String portName = importerArguments.getParentCommandOptions().get("target");
        port.setRef(String.format("ports/%s/%s.json", importerArguments.getParentCommandOptions().get("to"), portName));
        port.setName(portName);
        port.setVersion(parameters.get(PARAM_PORT_VERSION));

        Promises promises = port.getPromises() != null ? port.getPromises() : new Promises();
        port.setPromises(promises);
        promises.setPlatform(parameters.get(PARAM_PLATFORM));
        promises.setServicesType("datastore-services");

        StandardDefinition api = promises.getApi() != null ? promises.getApi() : new StandardDefinition();
        promises.setApi(api);
        api.setName(portName);
        api.setVersion(parameters.get(PARAM_PORT_VERSION));
        api.setSpecification("datastoreapi");
        api.setSpecificationVersion("1.0.0");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ObjectNode newDataStoreApiDefinitionJsonNode = objectMapper.valueToTree(dataStoreApiDefinition);

        JsonNode mergedApiDefinition = api.getDefinition() != null && !api.getDefinition().getAdditionalProperties().isEmpty() ?
                mergeDataStoreApi(objectMapper.valueToTree(api.getDefinition()), newDataStoreApiDefinitionJsonNode) :
                newDataStoreApiDefinitionJsonNode;
        try {
            api.setDefinition(objectMapper.treeToValue(mergedApiDefinition, ComponentBase.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return port;
    }


    private void validateRequiredParameters() {
        List<String> requiredParams = getExtensionOptions().stream().filter(ExtensionOption::isRequired).map(ExtensionOption::getNames).flatMap(Collection::stream).collect(Collectors.toList());

        for (String param : requiredParams) {
            if (parameters.get(param) == null) {
                throw new RuntimeException("Missing required parameter: " + param);
            }
        }
    }

    public void validateUniqueTableNames(DataStoreApiSchemaResource dataStoreApiSchemaResource) {
        List<String> tableNames = dataStoreApiSchemaResource.getTables().stream().map(table -> table.getDefinition().getName()).collect(Collectors.toList());

        Set<String> uniqueNames = new HashSet<>();
        for (String name : tableNames) {
            if (!uniqueNames.add(name)) {
                throw new RuntimeException("Duplicated table name found: please specify the correct catalog.");
            }
        }
    }

    @Override
    public List<ExtensionOption> getExtensionOptions() {
        return List.of(
                createRequiredOption(PARAM_PORT_VERSION, "The version of the port"),
                createRequiredOption(PARAM_PLATFORM, "The name of the platform"),
                createOptionWithDefault(PARAM_CATALOG_NAME, "The catalog regex to fetch JDBC metadata", null),
                createRequiredOption(PARAM_SCHEMA_NAME, "The schema name to fetch JDBC metadata"),
                createOptionWithDefault(PARAM_TABLES_REGEX, "The tables pattern to fetch JDBC metadata", "%"),
                createOptionWithDefault(PARAM_TABLE_TYPES, "The table type list to fetch JDBC metadata", "TABLE,VIEW")
        );
    }

    @Override
    public ExtensionInfo getExtensionInfo() {
        return new ExtensionInfo.Builder()
                .description("Extension to import a simple output port from a JDBC connection")
                .build();
    }

    private ExtensionOption createRequiredOption(String name, String description) {
        return new ExtensionOption.Builder()
                .names(name)
                .description(description)
                .required(true)
                .interactive(true)
                .setter(value -> parameters.put(name, value))
                .getter(() -> parameters.get(name))
                .build();
    }

    private ExtensionOption createOptionWithDefault(String name, String description, String defaultValue) {
        return new ExtensionOption.Builder()
                .names(name)
                .description(description)
                .required(false)
                .interactive(true)
                .setter(value -> parameters.put(name, value))
                .getter(() -> parameters.get(name))
                .defaultValue(defaultValue)
                .build();
    }

}
