# Importer JDBC Extension

## Overview
The **ImporterJDBCExtension** is a command-line extension for the Open Data Mesh CLI that allows importing database metadata using JDBC. It extracts schema, table, and column details and integrates them into a data product.

## Parent Command Parameters
The extension utilizes parameters from its parent command, which provides essential configuration details. These parameters are passed as part of `ImporterArguments` and include:

| Parameter  | Description |
|------------|-------------|
| `from`     | Specifies the source type (should be set to `jdbc` for this extension). |
| `to`       | Defines the destination port where the imported metadata is stored. |
| `target`   | The name of the target port for data import. |
| `source`   | The name of the system configuration containing JDBC connection details. |

These parameters are extracted from the Open Data Mesh CLI configuration (`OdmCliBaseConfiguration`). The system configurations define connection details, including:
- `name`: Identifier for the system.
- `endpoint`: JDBC URL for database connection.
- `user`: Database username.
- `password`: Database password.

## Extension Command Arguments
In addition to the parent command parameters, the **ImporterJDBCExtension** supports the following specific arguments:

| Argument        | Description |
|----------------|-------------|
| `--schemaName`  | Specifies the database schema to be imported. |
| `--catalogName` | Specifies the database catalog (optional, can be `null`). |
| `--tablesPattern` | Defines a pattern to filter tables (default: `%`, meaning all tables). |
| `--portVersion` | Version of the data product port being generated. |
| `--platform` | Defines the platform name, formatted as `dbType:platformName` (e.g., `h2:testplatform`). |

## Example Usage
Below is an example command using the extension with both parent command parameters and extension-specific arguments:

```sh
odm-cli local import \
  --from jdbc \
  --to output-port \
  --target test-port \
  --source testConnection \
  --schemaName TEST_SCHEMA \
  --catalogName null \
  --tablesPattern % \
  --portVersion 1.0.1 \
  --platform h2:testplatform
```

## Expected Behavior
1. The extension connects to the database using JDBC details from `source`.
2. It retrieves metadata from the specified schema and tables.
3. It constructs a `PortDPDS` object containing extracted metadata.
4. The metadata is stored in the specified `target` port, ensuring compatibility with Open Data Mesh standards.

## Testing
The `ImporterJDBCExtensionTest` validates the functionality:
- Ensures that metadata extraction includes expected tables and columns.
- Confirms correct reference generation for imported metadata.
- Uses an in-memory H2 database for testing purposes.