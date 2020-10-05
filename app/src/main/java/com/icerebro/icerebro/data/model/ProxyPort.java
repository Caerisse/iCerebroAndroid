package com.icerebro.icerebro.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProxyPort {
    @SerializedName("host")
    @Expose
    private String host;
    @SerializedName("port")
    @Expose
    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
