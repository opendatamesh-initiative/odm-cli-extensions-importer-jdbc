package org.opendatamesh.cli.extensions.importerjdbc.datastoreapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataStoreApiSchemaColumn {

    private String type;
    private String description;
    private String name;
    private String kind;
    private Boolean required;
    private String displayName;
    private String summary;
    private String comments;
    private List<String> examples;
    private String status;
    private List<String> tags;
    private String externalDocs;
    private String defaultValue;
    private Boolean isClassified;
    private String classificationLevel;
    private Boolean isUnique;
    private Boolean isNullable;
    private String pattern;
    private String format;
    private List<String> enumValues;
    private Integer minLength;
    private Integer maxLength;
    private String contentEncoding;
    private String contentMediaType;
    private Integer precision;
    private Integer scale;
    private Integer minimum;
    private Boolean exclusiveMinimum;
    private Integer maximum;
    private Boolean exclusiveMaximum;
    private Boolean readOnly;
    private Boolean writeOnly;
    private String physicalType;
    private Boolean partitionStatus;
    private Integer partitionKeyPosition;
    private Boolean clusterStatus;
    private Integer clusterKeyPosition;

    public DataStoreApiSchemaColumn() {

    }

    @JsonCreator
    public DataStoreApiSchemaColumn(
            @JsonProperty("type") String type,
            @JsonProperty("description") String description,
            @JsonProperty("name") String name,
            @JsonProperty("kind") String kind,
            @JsonProperty("required") Boolean required,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("summary") String summary,
            @JsonProperty("comments") String comments,
            @JsonProperty("examples") List<String> examples,
            @JsonProperty("status") String status,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("externalDocs") String externalDocs,
            @JsonProperty("default") String defaultValue,
            @JsonProperty("isClassified") Boolean isClassified,
            @JsonProperty("classificationLevel") String classificationLevel,
            @JsonProperty("isUnique") Boolean isUnique,
            @JsonProperty("isNullable") Boolean isNullable,
            @JsonProperty("pattern") String pattern,
            @JsonProperty("format") String format,
            @JsonProperty("enum") List<String> enumValues,
            @JsonProperty("minLength") Integer minLength,
            @JsonProperty("maxLength") Integer maxLength,
            @JsonProperty("contentEncoding") String contentEncoding,
            @JsonProperty("contentMediaType") String contentMediaType,
            @JsonProperty("precision") Integer precision,
            @JsonProperty("scale") Integer scale,
            @JsonProperty("minimum") Integer minimum,
            @JsonProperty("exclusiveMinimum") Boolean exclusiveMinimum,
            @JsonProperty("maximum") Integer maximum,
            @JsonProperty("exclusiveMaximum") Boolean exclusiveMaximum,
            @JsonProperty("readOnly") Boolean readOnly,
            @JsonProperty("writeOnly") Boolean writeOnly,
            @JsonProperty("physicalType") String physicalType,
            @JsonProperty("partitionStatus") Boolean partitionStatus,
            @JsonProperty("partitionKeyPosition") Integer partitionKeyPosition,
            @JsonProperty("clusterStatus") Boolean clusterStatus,
            @JsonProperty("clusterKeyPosition") Integer clusterKeyPosition) {
        this.type = type;
        this.description = description;
        this.name = name;
        this.kind = kind;
        this.required = required;
        this.displayName = displayName;
        this.summary = summary;
        this.comments = comments;
        this.examples = examples;
        this.status = status;
        this.tags = tags;
        this.externalDocs = externalDocs;
        this.defaultValue = defaultValue;
        this.isClassified = isClassified;
        this.classificationLevel = classificationLevel;
        this.isUnique = isUnique;
        this.isNullable = isNullable;
        this.pattern = pattern;
        this.format = format;
        this.enumValues = enumValues;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.contentEncoding = contentEncoding;
        this.contentMediaType = contentMediaType;
        this.precision = precision;
        this.scale = scale;
        this.minimum = minimum;
        this.exclusiveMinimum = exclusiveMinimum;
        this.maximum = maximum;
        this.exclusiveMaximum = exclusiveMaximum;
        this.readOnly = readOnly;
        this.writeOnly = writeOnly;
        this.physicalType = physicalType;
        this.partitionStatus = partitionStatus;
        this.partitionKeyPosition = partitionKeyPosition;
        this.clusterStatus = clusterStatus;
        this.clusterKeyPosition = clusterKeyPosition;
    }

    public Integer getClusterKeyPosition() {
        return clusterKeyPosition;
    }

    public void setClusterKeyPosition(Integer clusterKeyPosition) {
        this.clusterKeyPosition = clusterKeyPosition;
    }

    public Boolean isClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(Boolean clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public Integer getPartitionKeyPosition() {
        return partitionKeyPosition;
    }

    public void setPartitionKeyPosition(Integer partitionKeyPosition) {
        this.partitionKeyPosition = partitionKeyPosition;
    }

    public Boolean isPartitionStatus() {
        return partitionStatus;
    }

    public void setPartitionStatus(Boolean partitionStatus) {
        this.partitionStatus = partitionStatus;
    }

    public String getPhysicalType() {
        return physicalType;
    }

    public void setPhysicalType(String physicalType) {
        this.physicalType = physicalType;
    }

    public Boolean isWriteOnly() {
        return writeOnly;
    }

    public void setWriteOnly(Boolean writeOnly) {
        this.writeOnly = writeOnly;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public Boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public String getContentMediaType() {
        return contentMediaType;
    }

    public void setContentMediaType(String contentMediaType) {
        this.contentMediaType = contentMediaType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Boolean isNullable() {
        return isNullable;
    }

    public void setNullable(Boolean nullable) {
        isNullable = nullable;
    }

    public Boolean isUnique() {
        return isUnique;
    }

    public void setUnique(Boolean unique) {
        isUnique = unique;
    }

    public String getClassificationLevel() {
        return classificationLevel;
    }

    public void setClassificationLevel(String classificationLevel) {
        this.classificationLevel = classificationLevel;
    }

    public Boolean isClassified() {
        return isClassified;
    }

    public void setClassified(Boolean classified) {
        isClassified = classified;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getExternalDocs() {
        return externalDocs;
    }

    public void setExternalDocs(String externalDocs) {
        this.externalDocs = externalDocs;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
