package com.mcssapi.barebones.scheduler;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class getSchedulerTask {

    /**
     * @Param: IP address of the MCSS API server, including the port
     * @Param: ApiKey of the MCSS API server
     * @Param: ServerId of the server to get the info of
     * @Param: taskId of the task to get the info of
     * @param: SSL (true/false)
     * @return the server information as a JSON object. Null if error during request.
     */
    public static JSONObject getSchedulerTask(String IP, String ApiKey, String ServerId, String taskId, Boolean SSL) {
        //GET /api/v1/servers/{serverId}/scheduler/tasks/{taskId}

        URL url;
        HttpURLConnection conn;
        try {
            if (SSL) {
                url = new URL("https://" + IP + "/api/v1/servers/" + ServerId + "/scheduler/tasks/" + taskId);
            } else {
                url = new URL("http://" + IP + "/api/v1/servers/" + ServerId + "/scheduler/tasks/" + taskId);
            }
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
            conn.setReadTimeout(5000);
            conn.setRequestProperty("APIKey", ApiKey);
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Error: " + responseCode);
            }
            return new JSONObject(conn.getResponseMessage());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}