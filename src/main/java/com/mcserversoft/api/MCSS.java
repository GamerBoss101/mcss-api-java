package com.mcserversoft.api;

import java.util.ArrayList;

import org.json.JSONObject;

import com.mcserversoft.api.servers.ServerCountFilter;
import com.mcserversoft.api.servers.ServerFilter;
import com.mcserversoft.api.servers.ServerType;
import com.mcserversoft.api.servers.Servers;
import com.mcserversoft.api.users.Users;
import com.mcserversoft.api.utilities.Request;
import com.mcserversoft.commons.responses.Response;
import com.mcserversoft.commons.responses.client.ServerCountResponse;
import com.mcserversoft.commons.responses.client.ServersResponse;
import com.mcserversoft.commons.responses.client.SettingsResponse;
import com.mcserversoft.commons.responses.client.StatsResponse;
import com.mcserversoft.commons.responses.server.ServerResponse;

public class MCSS {

    private String url;
    private int port;
    private boolean https;
    private String apiKey;

    private static Request request;

    public Servers servers;
    public Users users;

    public MCSS(String ip, int port, String apiKey, boolean https) {
        this.port = port;
        this.https = https;
        this.apiKey = apiKey;

        String protocol = https ? "https" : "http";
        String portString = (port > 0) ? (":" + port) : "";
        this.url = protocol + "://" + ip + portString + "/api/v2";

        request = new Request(url);
        request.addHeader("apiKey", apiKey);

        this.servers = new Servers();
        this.users = new Users();
    }

    public MCSS(String ip, int port, String apiKey) {
        this(ip, port, apiKey, false);
    }
    
    public StatsResponse getStats() throws Exception {
        return new StatsResponse(request.GET("/"));
    }

    public ArrayList<ServerResponse> getServers() throws Exception {
        ServersResponse servers = new ServersResponse(request.GET("/servers"));
        return servers.getServers();
    }

    public ArrayList<ServerResponse> getServers(ServerFilter filter) throws Exception {
        ServersResponse servers = new ServersResponse(request.GET("/servers?filter=" + filter));
        return servers.getServers();
    }

    public ArrayList<ServerResponse> getServers(int filter) throws Exception {
        ServersResponse servers = new ServersResponse(request.GET("/servers?filter=" + filter));
        return servers.getServers();
    }

    public int getServerCount() throws Exception {
        return new ServerCountResponse(request.GET("/servers/count")).getCount();
    }

    public int getServerCount(ServerCountFilter filter) throws Exception {
        if(filter == ServerCountFilter.BYSERVERTYPE) throw new Exception("ServerCountFilter.BYSERVERTYPE is not supported yet");
        return new ServerCountResponse(request.GET("/servers/count?filter=" + filter)).getCount();
    }

    public int getServerCount(ServerCountFilter filter, ServerType type) throws Exception {
        return new ServerCountResponse(request.GET("/servers/count?filter=" + filter + "&type=" + type)).getCount();
    }

    public int getServerCount(int filter) throws Exception {
        if(filter == ServerCountFilter.BYSERVERTYPE.getValue()) throw new Exception("ServerCountFilter.BYSERVERTYPE is not supported yet");
        return new ServerCountResponse(request.GET("/servers/count?filter=" + filter)).getCount();
    }

    public int getServerCount(int filter, String type) throws Exception {
        return new ServerCountResponse(request.GET("/servers/count?filter=" + filter + "&type=" + type)).getCount();
    }

    public int getServerCount(int filter, ServerType type) throws Exception {
        return new ServerCountResponse(request.GET("/servers/count?filter=" + filter + "&type=" + type)).getCount();
    }

    public SettingsResponse getSettings() throws Exception {
        return new SettingsResponse(request.GET("/mcss/settings/All"));
    }

    public Response updateSettings(int deleteOldBackupsThreshold) throws Exception {
        return new Response(request.PATCH("/mcss/settings", new JSONObject().put("deleteOldBackupsThreshold", deleteOldBackupsThreshold)));
    }

    public String getUrl() { return this.url; }
    public int getPort() { return this.port; }
    public boolean isHttps() { return this.https; }
    public String getApiKey() { return this.apiKey; }

    public void setUrl(String ip) {
        String protocol = this.https ? "https" : "http";
        String portString = (this.port > 0) ? (":" + this.port) : "";
        this.url = protocol + "://" + ip + portString + "/api/v2";

    }
    
    public void setPort(int port) {
        this.port = port;
        String protocol = this.https ? "https" : "http";
        String portString = (this.port > 0) ? (":" + this.port) : "";
        this.url = protocol + "://" + this.url.split("://")[1].split(":")[0] + portString + "/api/v2";
        request.setBaseUrl(this.url);
    }

    public void setHttps(boolean https) {
        this.https = https;
        String protocol = this.https ? "https" : "http";
        String portString = (this.port > 0) ? (":" + this.port) : "";
        this.url = protocol + "://" + this.url.split("://")[1].split(":")[0] + portString + "/api/v2";
        request.setBaseUrl(this.url);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        request.setHeader("apiKey", apiKey);
    }

    public static Request getRequest() {
        return request;
    }
}
