package org.ohmage.funf;

import com.google.gson.JsonObject;
import org.joda.time.DateTime;

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

    public JsonObject getJson() {
        JsonObject j = data;
        j.add("probeConfig", config);
        j.addProperty("timestamp", timestamp);
        j.addProperty("device_info", build);
        return j;
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
