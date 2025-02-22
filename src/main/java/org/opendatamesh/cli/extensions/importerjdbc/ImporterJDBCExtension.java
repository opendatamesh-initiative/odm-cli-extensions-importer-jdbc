package org.opendatamesh.cli.extensions.importerjdbc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatamesh.cli.extensions.ExtensionInfo;
import org.opendatamesh.cli.extensions.ExtensionOption;
import org.opendatamesh.cli.extensions.OdmCliBaseConfiguration;
import org.opendatamesh.cli.extensions.importer.ImporterArguments;
import org.opendatamesh.cli.extensions.importer.ImporterExtension;
import org.opendatamesh.cli.extensions.importerjdbc.datastoreapi.*;
import org.opendatamesh.dpds.model.core.StandardDefinitionDPDS;
import org.opendatamesh.dpds.model.interfaces.PortDPDS;
import org.opendatamesh.dpds.model.interfaces.PromisesDPDS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ImporterJDBCExtension implements ImporterExtension<PortDPDS> {

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
    public Class<PortDPDS> getTargetClass() {
        return PortDPDS.class;
    }

    @Override
    public PortDPDS importElement(PortDPDS targetObject, ImporterArguments importerArguments) {

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

        PortDPDS portDPDS = new PortDPDS();
        String portName = importerArguments.getParentCommandOptions().get("target");
        portDPDS.setRef(String.format("ports/%s/%s.json", importerArguments.getParentCommandOptions().get("to"), portName));
        portDPDS.setName(portName);
        portDPDS.setVersion(parameters.get(PARAM_PORT_VERSION));

        PromisesDPDS promises = new PromisesDPDS();
        portDPDS.setPromises(promises);
        promises.setPlatform(parameters.get(PARAM_PLATFORM));
        promises.setServicesType("datastore-services");

        StandardDefinitionDPDS api = new StandardDefinitionDPDS();
        promises.setApi(api);
        api.setName(portName);
        api.setVersion(parameters.get(PARAM_PORT_VERSION));
        api.setSpecification("datastoreapi");
        api.setSpecificationVersion("1.0.0");

        dataStoreApiDefinition.setDatastoreapi("1.0.0");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        api.setDefinition(objectMapper.valueToTree(dataStoreApiDefinition));

        return portDPDS;
    }

    private void validateRequiredParameters() {
        List<String> requiredParams = getExtensionOptions().stream()
                .filter(ExtensionOption::isRequired)
                .map(ExtensionOption::getNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (String param : requiredParams) {
            if (parameters.get(param) == null) {
                throw new RuntimeException("Missing required parameter: " + param);
            }
        }
    }

    public void validateUniqueTableNames(DataStoreApiSchemaResource dataStoreApiSchemaResource) {
        List<String> tableNames = dataStoreApiSchemaResource.getTables().stream()
                .map(table -> table.getDefinition().getName())
                .collect(Collectors.toList());

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
                createOption(PARAM_PORT_VERSION, "The version of the port", true),
                createOption(PARAM_PLATFORM, "The name of the platform", true),
                createOptionWithDefault(PARAM_CATALOG_NAME, "The catalog regex to fetch JDBC metadata", false, null),
                createOption(PARAM_SCHEMA_NAME, "The schema name to fetch JDBC metadata", true),
                createOptionWithDefault(PARAM_TABLES_REGEX, "The tables pattern to fetch JDBC metadata", false, "%"),
                createOptionWithDefault(PARAM_TABLE_TYPES, "The table type list to fetch JDBC metadata", false, "TABLE,VIEW")
        );
    }

    @Override
    public ExtensionInfo getExtensionInfo() {
        return new ExtensionInfo.Builder()
                .description("Extension to import a simple output port from a JDBC connection")
                .build();
    }

    private ExtensionOption createOption(String name, String description, boolean required) {
        return new ExtensionOption.Builder()
                .names(name)
                .description(description)
                .required(required)
                .interactive(true)
                .setter(value -> parameters.put(name, value))
                .getter(() -> parameters.get(name))
                .build();
    }

    private ExtensionOption createOptionWithDefault(String name, String description, boolean required, String defaultValue) {
        return new ExtensionOption.Builder()
                .names(name)
                .description(description)
                .required(required)
                .interactive(true)
                .setter(value -> parameters.put(name, value))
                .getter(() -> parameters.get(name))
                .defaultValue(defaultValue)
                .build();
    }
}
