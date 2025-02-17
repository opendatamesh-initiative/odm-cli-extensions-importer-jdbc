package org.opendatamesh.cli.extensions.importerjdbc.datastoreapi;

public class DataStoreApiDefinition {
    private String datastoreapi;
    private DataStoreApiInfo info;
    private DataStoreApiSchema schema;

    public String getDatastoreapi() {
        return datastoreapi;
    }

    public void setDatastoreapi(String datastoreapi) {
        this.datastoreapi = datastoreapi;
    }

    public DataStoreApiInfo getInfo() {
        return info;
    }

    public void setInfo(DataStoreApiInfo info) {
        this.info = info;
    }

    public DataStoreApiSchema getSchema() {
        return schema;
    }

    public void setSchema(DataStoreApiSchema schema) {
        this.schema = schema;
    }
}
