package org.opendatamesh.cli.extensions.importerjdbc;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.*;
import org.opendatamesh.cli.extensions.importer.ImporterArguments;
import org.opendatamesh.cli.extensions.OdmCliBaseConfiguration;
import org.opendatamesh.dpds.model.interfaces.PortDPDS;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    void testImportElement() {
        PortDPDS port = importerJDBC.importElement(new PortDPDS(), importerArguments);

        assertNotNull(port);
        assertNotNull(port.getPromises());
        assertNotNull(port.getPromises().getApi());
        assertNotNull(port.getPromises().getApi().getDefinitionJson());
        assertEquals("ports/output-port/test-port.json", port.getRef());

        String jsonDefinition = port.getPromises().getApi().getDefinitionJson().toString();
        assertTrue(jsonDefinition.contains("test_table".toUpperCase()), "Extracted metadata should contain 'test_table'");
        assertTrue(jsonDefinition.contains("id".toUpperCase()), "Extracted metadata should contain column 'id'");
        assertTrue(jsonDefinition.contains("name".toUpperCase()), "Extracted metadata should contain column 'name'");
        assertTrue(jsonDefinition.contains("created_at".toUpperCase()), "Extracted metadata should contain column 'created_at'");
    }
}
