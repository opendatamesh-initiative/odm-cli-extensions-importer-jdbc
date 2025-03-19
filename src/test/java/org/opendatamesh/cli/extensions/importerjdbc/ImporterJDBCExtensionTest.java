package org.opendatamesh.cli.extensions.importerjdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.cli.extensions.OdmCliBaseConfiguration;
import org.opendatamesh.cli.extensions.importer.ImporterArguments;
import org.opendatamesh.dpds.model.core.ComponentBase;
import org.opendatamesh.dpds.model.interfaces.Port;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ImporterJDBCExtensionTest {

    private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    private ImporterJDBCExtension importerJDBC;
    private ImporterArguments importerArguments;

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            RunScript.execute(conn, new StringReader(
                    "CREATE SCHEMA IF NOT EXISTS test_schema;\n" +
                            "CREATE TABLE test_schema.test_table (\n" +
                            "    id INT PRIMARY KEY,\n" +
                            "    name VARCHAR(255) NOT NULL,\n" +
                            "    created_at TIMESTAMP\n" +
                            ");"
            ));
        }
    }

    @BeforeEach
    void setup() {
        importerJDBC = new ImporterJDBCExtension();
        importerJDBC.getExtensionOptions().forEach(option -> importerJDBC.getExtensionOptions());

        OdmCliBaseConfiguration.SystemConfig systemConfig = new OdmCliBaseConfiguration.SystemConfig();
        systemConfig.setName("testConnection");
        systemConfig.setEndpoint(JDBC_URL);
        systemConfig.setUser(JDBC_USER);
        systemConfig.setPassword(JDBC_PASSWORD);

        OdmCliBaseConfiguration config = new OdmCliBaseConfiguration();
        config.setSystems(List.of(systemConfig));

        importerArguments = new ImporterArguments();
        importerArguments.setParentCommandOptions(Map.of(
                "target", "test-port",
                "source", "testConnection",
                "from", "jdbc",
                "to", "output-port"
        ));

        importerArguments.setOdmCliConfig(config);
        Map<String, String> params = new HashMap<>();
        params.put("--schemaName", "TEST_SCHEMA");
        params.put("--catalogName", null);
        params.put("--tablesPattern", "%");
        params.put("--portVersion", "1.0.1");
        params.put("--platform", "h2:testplatform");

        importerJDBC.getExtensionOptions().forEach(option -> option.getSetter().accept(params.get(option.getNames().get(0))));
    }

    @Test
    void testImportElement() throws JsonProcessingException {
        Port port = importerJDBC.importElement(null, importerArguments);

        assertNotNull(port);
        assertNotNull(port.getPromises());
        assertNotNull(port.getPromises().getApi());
        assertNotNull(port.getPromises().getApi().getDefinition());
        assertEquals("ports/output-port/test-port.json", port.getRef());

        String jsonDefinition = new ObjectMapper().writeValueAsString(port.getPromises().getApi().getDefinition());
        assertTrue(jsonDefinition.contains("test_table".toUpperCase()), "Extracted metadata should contain 'test_table'");
        assertTrue(jsonDefinition.contains("id".toUpperCase()), "Extracted metadata should contain column 'id'");
        assertTrue(jsonDefinition.contains("name".toUpperCase()), "Extracted metadata should contain column 'name'");
        assertTrue(jsonDefinition.contains("created_at".toUpperCase()), "Extracted metadata should contain column 'created_at'");
    }

    @Test
    void testImportElementWithPatch() throws JsonProcessingException {
        Port existing = loadPortFromTestResources();
        Port port = importerJDBC.importElement(existing, importerArguments);

        assertNotNull(port);
        assertNotNull(port.getPromises());
        assertNotNull(port.getPromises().getApi());
        assertNotNull(port.getPromises().getApi().getDefinition());
        assertEquals("ports/output-port/test-port.json", port.getRef());

        String jsonDefinition = new ObjectMapper().writeValueAsString(port.getPromises().getApi().getDefinition());
        assertTrue(jsonDefinition.contains("TEST_TABLE"), "Extracted metadata should contain 'TEST_TABLE'");
        assertTrue(jsonDefinition.contains("ID"), "Extracted metadata should contain column 'ID'");
        assertTrue(jsonDefinition.contains("NAME"), "Extracted metadata should contain column 'NAME'");
        assertTrue(jsonDefinition.contains("CREATED_AT"), "Extracted metadata should contain column 'CREATED_AT'");
        assertFalse(jsonDefinition.contains("to_be_removed"), "Extracted metadata should not contain text 'to_be_removed'");
        assertFalse(jsonDefinition.contains("should-be-updated"), "Extracted metadata should not contain text 'should-be-updated'");

        assertTrue(jsonDefinition.contains("preserved-table-property"), "Extracted metadata should contain text 'preserved-table-property'");
        assertTrue(jsonDefinition.contains("preserved-column-property"), "Extracted metadata should contain text 'preserved-column-property'");
        assertTrue(jsonDefinition.contains("preserved-table-description"), "Extracted metadata should contain text 'preserved-table-description'");
        assertTrue(jsonDefinition.contains("preserved-column-description"), "Extracted metadata should contain text 'preserved-column-description'");


    }

    private Port loadPortFromTestResources() {
        try {
            String text = new Scanner(Objects.requireNonNull(ImporterJDBCExtensionTest.class.getResourceAsStream("ImporterJDBCExtensionTest.testPatch.json")), StandardCharsets.UTF_8).useDelimiter("\\A").next();
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonNode = objectMapper.readValue(text, ObjectNode.class);
            JsonNode datastoreSchema = jsonNode.path("promises").path("api").path("definition");

            Port port = objectMapper.readValue(text, Port.class);
            port.getPromises().getApi().setDefinition(new ObjectMapper().treeToValue(datastoreSchema, ComponentBase.class));
            return port;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
