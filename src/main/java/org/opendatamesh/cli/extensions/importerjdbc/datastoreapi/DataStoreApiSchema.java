package org.opendatamesh.cli.extensions.importerjdbc.datastoreapi;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = DataStoreApiSchemaDeserializer.class)
public abstract class DataStoreApiSchema {
}
