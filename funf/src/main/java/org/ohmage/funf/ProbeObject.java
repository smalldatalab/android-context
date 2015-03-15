package org.ohmage.funf;

import com.google.gson.JsonObject;

import java.util.UUID;

public class ProbeObject {

    private JsonObject data, config;
    private String timestamp;
    private String build;
    private int id;

    public ProbeObject(JsonObject data, JsonObject config, String timestamp, String build) {
        this.data = data;
        this.config = config;
        this.timestamp = timestamp;
        this.build = build;
    }

    public ProbeObject(int id, JsonObject data, JsonObject config, String timestamp, String build) {
        this.data = data;
        this.config = config;
        this.timestamp = timestamp;
        this.build = build;
        this.id = id;
    }

    private static final JsonObject schemaIdJson;
    private static final JsonObject acquisitionProvenance;
    static {
        schemaIdJson = new JsonObject();
        schemaIdJson.addProperty("namespace", "io.smalldatalab");
        schemaIdJson.addProperty("name","context");
        schemaIdJson.addProperty("version", "1.0");
        acquisitionProvenance = new JsonObject();
        acquisitionProvenance.addProperty("source_name", "Context-1.0");
        acquisitionProvenance.addProperty("modality", "sensed");
    };
    public JsonObject getJson() {

        JsonObject header = new JsonObject();
        header.addProperty("id", String.valueOf(UUID.randomUUID()));
        header.addProperty("creation_date_time", timestamp);
        header.add("schema_id", schemaIdJson);
        header.add("acquisition_provenance", acquisitionProvenance);

        JsonObject body = new JsonObject();
        body.add("data", data);
        body.add("probe_config", config);
        body.addProperty("device_info", build);


        JsonObject datapoint = new JsonObject();
        datapoint.add("header", header);
        datapoint.add("body", body);

        return datapoint;
    }

    public int getID() {
        return id;
    }

    public String getData() {
        return data.toString();
    }

    public String getConfig() {
        return config.toString();
    }

    public String getTimestamp() {
        return timestamp.toString();
    }

    public String getBuild() {
        return build;
    }
}
