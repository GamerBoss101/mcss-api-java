package com.mcserversoft.api.backups;

import java.util.ArrayList;

import org.json.JSONObject;

import com.mcserversoft.api.MCSS;
import com.mcserversoft.api.utilities.Request;
import com.mcserversoft.commons.responses.Response;
import com.mcserversoft.commons.responses.backup.BackupResponse;
import com.mcserversoft.commons.responses.backup.BackupsResponse;
import com.mcserversoft.commons.structures.BackupBuilder;

public class Backups {
 
    private Request request;

    private String serverId;

    public Backups(String serverId) {
        this.request = MCSS.getRequest();
        this.serverId = null;
    }

    public BackupResponse get() throws Exception {
        return new BackupResponse(this.request.GET("/servers/" + this.serverId + "/backups/stats"));
    }

    public ArrayList<Backup> getBackups() throws Exception {
        return new BackupsResponse(this.request.GET("/servers/" + this.serverId + "/backups")).getBackups();
    }

    public Backup getBackup(String backupId) throws Exception {
        return new Backup(this.request.GET("/servers/" + this.serverId + "/backups/" + backupId));
    }

    public Response create(BackupBuilder backup) throws Exception {
        return new Response(this.request.POST("/servers/" + this.serverId + "/backups", backup.toJSON()));
    }

    public Response create(JSONObject backup) throws Exception {
        return new Response(this.request.POST("/servers/" + this.serverId + "/backups", backup));
    }

    public Response update(String backupId, BackupBuilder backup) throws Exception {
        return new Response(this.request.PUT("/servers/" + this.serverId + "/backups/" + backupId, backup.toJSON()));
    }

    public Response update(String backupId, JSONObject backup) throws Exception {
        return new Response(this.request.PUT("/servers/" + this.serverId + "/backups/" + backupId, backup));
    }

    public Response delete(String backupId) throws Exception {
        return new Response(this.request.DELETE("/servers/" + this.serverId + "/backups/" + backupId));
    }

    public Response run(String backupId) throws Exception {
        return new Response(this.request.POST("/servers/" + this.serverId + "/backups/" + backupId, new JSONObject()));
    }
    
    public Response getHistory() throws Exception {
        return new Response(this.request.GET("/servers/" + this.serverId + "/backups/history"));
    }

    public Response clearHistory() throws Exception {
        return new Response(this.request.DELETE("/servers/" + this.serverId + "/backups/history/clear"));
    }

}
