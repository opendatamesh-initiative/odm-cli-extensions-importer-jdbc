package org.opendatamesh.cli.extensions.importerjdbc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatamesh.cli.extensions.ExtensionInfo;
import org.opendatamesh.cli.extensions.ExtensionOption;
import org.opendatamesh.cli.extensions.OdmCliBaseConfiguration;
import org.opendatamesh.cli.extensions.importerjdbc.datastoreapi.*;
import org.opendatamesh.cli.extensions.importschema.ImportSchemaArguments;
import org.opendatamesh.cli.extensions.importschema.ImportSchemaExtension;
import org.opendatamesh.dpds.model.core.StandardDefinitionDPDS;
import org.opendatamesh.dpds.model.interfaces.PortDPDS;
import org.opendatamesh.dpds.model.interfaces.PromisesDPDS;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ImporterJDBCExtension implements ImportSchemaExtension {

    private static final String SUPPORTED_FROM = "jdbc";
    private static final String SUPPORTED_TO = "port";

    private static final String PARAM_CONNECTION_NAME = "--connectionName";
    private static final String PARAM_PORT_NAME = "--portName";
    private static final String PARAM_PORT_VERSION = "--portVersion";
    private static final String PARAM_PLATFORM = "--platform";
    private static final String PARAM_CATALOG_NAME = "--catalogName";
    private static final String PARAM_SCHEMA_NAME = "--schemaName";
    private static final String PARAM_TABLES_REGEX = "--tablesRegex";

    private final Map<String, String> parameters = new HashMap<>();

    @Override
    public boolean supports(String from, String to) {
        return SUPPORTED_FROM.equalsIgnoreCase(from) && SUPPORTED_TO.equalsIgnoreCase(to);
    }

    @Override
    public PortDPDS importElement(ImportSchemaArguments importSchemaArguments) {

        validateRequiredParameters();

        // Retrieve connection configuration
        OdmCliBaseConfiguration.System connection = importSchemaArguments.getOdmCliConfig()
                .getRemoteSystemsConfigurations()
                .stream()
                .filter(s -> parameters.get(PARAM_CONNECTION_NAME).equals(s.getName()))
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
        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalogName = parameters.get(PARAM_CATALOG_NAME);
            String schemaName = parameters.get(PARAM_SCHEMA_NAME);
            String tablePattern = parameters.get(PARAM_TABLES_REGEX);

            try (ResultSet tables = metaData.getTables(catalogName, schemaName, tablePattern, null)) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");

                    DataStoreApiSchemaEntity entity = new DataStoreApiSchemaEntity();
                    dataStoreApiSchemaResource.getTables().add(entity);
                    DataStoreAPISchemaEntityDefinition entityDefinition = new DataStoreAPISchemaEntityDefinition();
                    entity.setDefinition(entityDefinition);
                    entityDefinition.setName(tables.getString("TABLE_NAME"));
                    entityDefinition.setProperties(new HashMap<>());

                    try (ResultSet columns = metaData.getColumns(catalogName, schemaName, tableName, null)) {
                        while (columns.next()) {
                            DataStoreApiSchemaColumn columnMetadata = new DataStoreApiSchemaColumn();
                            columnMetadata.setName(columns.getString("COLUMN_NAME"));
                            columnMetadata.setType(SQLToJsonSchemaMapper.mapSqlTypeToJsonSchema(columns.getInt("DATA_TYPE")));
                            columnMetadata.setPhysicalType(columns.getString("TYPE_NAME"));
                            entityDefinition.getProperties().put(columnMetadata.getName(), columnMetadata);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving JDBC metadata", e);
        }

        PortDPDS portDPDS = new PortDPDS();
        portDPDS.setRef(String.format("ports/%s.json", parameters.get(PARAM_PORT_NAME)));
        portDPDS.setName(parameters.get(PARAM_PORT_NAME));
        portDPDS.setVersion(parameters.get(PARAM_PORT_VERSION));

        PromisesDPDS promises = new PromisesDPDS();
        portDPDS.setPromises(promises);
        promises.setPlatform(parameters.get(PARAM_PLATFORM));
        promises.setServicesType("datastore-services");

        StandardDefinitionDPDS api = new StandardDefinitionDPDS();
        promises.setApi(api);
        api.setName(parameters.get(PARAM_PORT_NAME));
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

    @Override
    public List<ExtensionOption> getExtensionOptions() {
        return List.of(
                createOption(PARAM_CONNECTION_NAME, "The name of the connection", true),
                createOption(PARAM_PORT_NAME, "The name of the port", true),
                createOption(PARAM_PORT_VERSION, "The version of the port", true),
                createOption(PARAM_PLATFORM, "The version of the port", true),
                createOption(PARAM_CATALOG_NAME, "The catalog regex to fetch JDBC metadata", false),
                createOption(PARAM_SCHEMA_NAME, "The schemas regex to fetch JDBC metadata", true),
                createOption(PARAM_TABLES_REGEX, "The tables regex to fetch JDBC metadata", false)
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
}
