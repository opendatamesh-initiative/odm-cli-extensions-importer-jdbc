package org.opendatamesh.cli.extensions.importerjdbc.datastoreapi;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(as = DataStoreApiSchemaResource.class)
public class DataStoreApiSchemaResource extends DataStoreApiSchema {
    private Integer id;
    private String name;
    private String version;
    private String mediaType;
    private String databaseSchemaName;
    private String databaseName;
    private List<DataStoreApiSchemaEntity> tables;


    public String getDatabaseSchemaName() {
        return databaseSchemaName;
    }

    public void setDatabaseSchemaName(String databaseSchemaName) {
        this.databaseSchemaName = databaseSchemaName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public List<DataStoreApiSchemaEntity> getTables() {
        return tables;
    }

    public void setTables(List<DataStoreApiSchemaEntity> tables) {
        this.tables = tables;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }


}


