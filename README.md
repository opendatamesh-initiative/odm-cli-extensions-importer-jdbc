# Importer JDBC Extension

## Overview
The **ImporterJDBCExtension** is a command-line extension for the Open Data Mesh CLI that allows importing database metadata using JDBC. It extracts schema, table, and column details and integrates them into a data product.

## How to Use

### Download ODM CLI
To run the application properly, you must have a Java JDK installed.

```sh
sudo apt update && sudo apt install -y openjdk-17-jdk
```

Download the CLI:

```bash
wget -qO odm-cli $(wget -qO- https://api.github.com/repos/opendatamesh-initiative/odm-cli/releases/latest | grep -Eo '"browser_download_url": *"[^"]+"' | grep odm-cli | sed -E 's/.*"([^"]+)".*/\1/' | head -n1) && chmod +x odm-cli
```

Test the CLI:

```bash
./odm-cli --version
```

## Configure Extensions
By default, `odmcli` stores its configuration file in a directory named `.odmcli` within your `$HOME` directory. This directory contains extension and configuration property files.

After running the command, the `.odmcli` folder should be created automatically. To customize the configuration, add or modify the `application.yml` file within this directory.

### Example Configuration
Below is an example `application.yml` file:

```yaml
cli:
  systems:
    - name: local-postgres
      endpoint: "jdbc:postgresql://localhost:5432/postgres?useSSL=false"
      user: "postgres"
      password: "change-your-secret-password"
extensions:
  - name: odm-cli-extensions-importer-jdbc
    url: https://github.com/opendatamesh-initiative/odm-cli-extensions-importer-jdbc/releases/download/v1.2.0/odm-cli-extensions-importer-jdbc-1.2.0.jar
  - name: postgresql-42.7.5
    url: https://jdbc.postgresql.org/download/postgresql-42.7.5.jar
```

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
./odm-cli local import \
  --target test-port \
  --source testConnection \
  --from jdbc \
  --to output-port \
  --schemaName TEST_SCHEMA \
  --catalogName null \
  --tablesPattern % \
  --portVersion 1.0.1 \
  --platform h2:testplatform
```

## Expected Behavior
1. The extension connects to the database using JDBC details from `source`.
2. It retrieves metadata from the specified schema and tables.
3. It constructs a `Port` object containing extracted metadata.
4. The metadata is stored in the specified `target` port, ensuring compatibility with Open Data Mesh standards.

## Testing
The `ImporterJDBCExtensionTest` validates the functionality:
- Ensures that metadata extraction includes expected tables and columns.
- Confirms correct reference generation for imported metadata.
- Uses an in-memory H2 database for testing purposes.
