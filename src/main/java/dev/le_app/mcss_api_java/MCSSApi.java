package dev.le_app.mcss_api_java;

import dev.le_app.mcss_api_java.exceptions.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * The main class of the API.
 */
public class MCSSApi {

    protected String IP = null;
    protected String token = null;
    protected String version = null;
    protected String expectedVersion = "2.0.0";

    protected Boolean allowUnsafeSSL = false;

    /**
     * Create a new MCSSApi object
     * @param IP The IP of the MCSS server
     * @param token The token of the MCSS server
     * @throws APIUnauthorizedException If the token is invalid
     * @throws APIVersionMismatchException If the API version of the MCSS server is not the same as the expected version
     * @throws IOException If there is an error connecting to the MCSS server
     */
    public MCSSApi(String IP, String token) throws APIUnauthorizedException, APIVersionMismatchException, IOException {
        this.IP = IP;
        this.token = token;

        Info in = getInfo();
        this.version = in.getMCSSApiVersion();
        checkVersionMismatch();
    }

    /**
     * Get the API object
     * @param IP The IP of the MCSS server
     * @param token The token of the MCSS server
     * @param allowUnsafeSSL True if you want to avoid checking the SSL certificate
     * @throws APIUnauthorizedException If the token is invalid
     * @throws APIVersionMismatchException If the API version is not the same as the expected version
     * @throws IOException If there is an error while connecting to the API
     * @throws KeyManagementException If there is an error with the KeyManagment
     * @throws NoSuchAlgorithmException If there is an error with the SSLContext
     */
    public MCSSApi(String IP, String token, Boolean allowUnsafeSSL) throws APIUnauthorizedException, APIVersionMismatchException, IOException, NoSuchAlgorithmException, KeyManagementException {
        this.IP = IP;
        this.token = token;
        this.allowUnsafeSSL = allowUnsafeSSL;

        if (allowUnsafeSSL) {
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            //Warn the user of the unsafe SSL connection
            System.out.println("[MCSSAPI] WARNING: Unsafe SSL connection enabled! NO SSL CERTIFICATES WILL BE VERIFIED!");

        }

        Info in = getInfo();
        this.version = in.getMCSSApiVersion();
        checkVersionMismatch();
    }

    /**
     * Resets all web panel sessions, meaning that every web panel user will be logged out,
     * and will have to log in again.
     */
    public void wipeSessions() throws IOException, APIUnauthorizedException, APIServerSideException, APINotFoundException {

        URL url = new URL (Endpoints.WIPE_SESSIONS.getEndpoint().replace("{IP}", IP));
        HttpURLConnection conn = createPostConnection(url);

        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode == 401 ) {
            throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
        } else if (responseCode == 403) {
            throw new APIUnauthorizedException(Errors.NOT_ADMIN.getMessage());
        } else if (responseCode == 500 ) {
            throw new APIServerSideException(Errors.API_ERROR.getMessage());
        } else if (responseCode == 404) {
            throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
        }

        conn.disconnect();
    }


    /**
     * Get general information about the MCSS install
     * @return Info object containing the information
     * @throws IOException General IO error
     * @throws APIUnauthorizedException API token is invalid/expired
     */
    public Info getInfo() throws IOException, APIUnauthorizedException {
        URL url;

        url = new URL(Endpoints.ROOT.getEndpoint().replace("{IP}", IP));
        HttpURLConnection conn = createGetConnection(url);

        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode == 401) {
            throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
        }

        //Read the response
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JSONObject json = new JSONObject(new JSONTokener(reader));

        //close connection
        conn.disconnect();

        return new Info(json.getBoolean("isDevBuild"), json.getString("mcssVersion"),
                json.getString("mcssApiVersion"), json.getString("uniqueIdentifier"),
                json.getBoolean("youAreAwesome"));
    }

    /**
     * Get the list of servers
     * @return ArrayList of servers
     * @throws APIUnauthorizedException API token is invalid/expired
     * @throws APINotFoundException API not found
     * @throws IOException General IO error
     * @throws APINoServerAccessException API does not have access to any server
     */
    public ArrayList<Server> getServers() throws APIUnauthorizedException, APINotFoundException, IOException, APINoServerAccessException {

        //create the ArrayList
        ArrayList<Server> servers = new ArrayList<>();

        //create the URL
        URL url = new URL(Endpoints.SERVERS.getEndpoint().replace("{IP}", IP));
        //Create and open the connection
        HttpURLConnection conn = createGetConnection(url);

        //Connect to the API
        conn.connect();
        //Get the response code of the connection
        int responseCode = conn.getResponseCode();
        //if the responsecode is an error, throw an exception
        if (responseCode == 401) {
            throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
        } else if (responseCode == 404) {
            //Might never fire, better safe than sorry
            throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
        } else if (responseCode == 403) {
            throw new APINoServerAccessException(Errors.NO_SERVER_ACCESS.getMessage());
        }

        //save the response in a JSONObject
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JSONArray json = new JSONArray(new JSONTokener(reader));

        //close connection
        conn.disconnect();
        //Create the JsonArray from the JSONObject
        JSONArray serversArray = new JSONArray(json);
        //Create a DateTimeFormatter to parse the creationDate
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        //loop through the JsonArray and create a Server object for each server
        for (int i = 0; i < serversArray.length(); i++) {
            JSONObject server = serversArray.getJSONObject(i);
            //Create the Server object with parsed values from JSON, and add it to the ArrayList
            servers.add(new Server(server.getString("guid"), this));
        }

        //return the ArrayList
        return servers;

    }

    private void checkVersionMismatch() throws APIVersionMismatchException {
        if (!Objects.equals(version, expectedVersion)) {
            throw new APIVersionMismatchException(Errors.VERSION_MISMATCH.getMessage().replace("{GOT}", version)
                    .replace("{EXPECTED_VERSION}", expectedVersion));
        }
    }


    /**
     * Get the number of servers.
     * @throws APIUnauthorizedException if the APIKey is invalid
     * @throws IOException if there is an error with the connection
     * @return number of servers
     */
    public int getServerCount() throws APIUnauthorizedException, IOException {
        URL url = new URL(Endpoints.SERVER_COUNT.getEndpoint().replace("{IP}", IP));

        HttpURLConnection conn = createGetConnection(url);

        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode == 401) {
            throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
        }

        //save the response in a JSONObject
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JSONObject json = new JSONObject(new JSONTokener(reader));

        //close connection
        conn.disconnect();
        return json.getInt("count");
    }

    /**
     * Get the number of servers.
     * @param filter the filter to use
     * @throws APIUnauthorizedException if the APIKey is invalid
     * @throws IOException if there is an error with the connection
     * @return number of servers
     */
    public int getServerCount(ServerFilter filter) throws APIUnauthorizedException, IOException {
        URL url = new URL( Endpoints.SERVER_COUNT_FILTER.getEndpoint().replace("{IP}", IP)
                .replace("{FILTER}", filter.getValueStr()));

        HttpURLConnection conn = createGetConnection(url);
        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode == 401) {
            throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
        }

        //save the response in a JSONObject
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JSONObject json = new JSONObject(new JSONTokener(reader));

        //close connection
        conn.disconnect();
        return json.getInt("count");
    }

    /**
     * Get the number of servers. Only used for the servertype filter.
     * @param filter the ServerFilter to use
     * @param serverTypeID Only required if the filter is FILTER
     * @throws APIUnauthorizedException if the APIKey is invalid
     * @throws IOException if there is an error with the connection
     * @return number of servers
     */
    public int getServerCount(ServerFilter filter, String serverTypeID) throws APIUnauthorizedException, IOException {

        if (filter != ServerFilter.FILTER) {
            throw new IllegalArgumentException(Errors.ID_FILTER_ERROR.getMessage());
        }

        URL url = new URL(Endpoints.SERVER_COUNT_FILTER_SRVTYPE.getEndpoint().replace("{IP}", IP)
                .replace("{FILTER}", filter.getValueStr())
                .replace("{SRVTYPE}", serverTypeID));

        HttpURLConnection conn = createGetConnection(url);

        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode == 401) {
            throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
        }

        //save the response in a JSONObject
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JSONObject json = new JSONObject(new JSONTokener(reader));

        //close connection
        conn.disconnect();
        return json.getInt("count");
    }

    /**
     * Get all of the users currently in the API.
     * @return ArrayList of User objects
     * @throws APIUnauthorizedException if the APIKey is invalid, or the user is not an admin
     * @throws IOException if there is an error with the connection
     * @throws APINotFoundException Consider this as an internal server error. It happens if the user found by the get request gets deleted before it is created inside the ArrayList.
     * @throws APIServerSideException Internal server error (500-or related response codes)
     */
    public ArrayList<User> getUsers() throws APIUnauthorizedException, IOException, APIServerSideException, APINotFoundException {

        ArrayList<User> users = new ArrayList<>();

        URL url = new URL(Endpoints.USERS.getEndpoint().replace("{IP}", IP));

        HttpURLConnection conn = createGetConnection(url);

        conn.connect();
        int responseCode = conn.getResponseCode();
        switch (responseCode) {
            case 401 -> throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 403 -> throw new APIUnauthorizedException(Errors.NOT_ADMIN.getMessage());
            case 200 -> {
                //save the response in a JSONObject
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JSONArray userArray = new JSONArray(new JSONTokener(reader));
                //close connection
                conn.disconnect();

                for (int i = 0; i < userArray.length(); i++) {
                    JSONObject user = userArray.getJSONObject(i);
                    users.add(new User(this, user.getString("userId")));
                }
            }
        }

        return users;

    }

    /**
     * Create an user on the API, for the web panel. The password is not cached by the API Wrapper.
     * @param user The user to create, already setup with the required fields and options
     * @param password The password for the user to access the web panel
     * @param repeatPassword The password repeated, to make sure it's correct
     * @return The created user, functional inside the API.
     * @throws IOException If there is an error with the connection
     * @throws APIServerSideException If the API returns an error 500 or related
     * @throws APIUnauthorizedException If the APIKey is invalid, OR the user is not an admin
     * @throws APINotFoundException Consider this as an internal server error. It happens if the user found by the get request gets deleted before it is returned to the user.
     * @throws APIInvalidUserException If the user is invalid, or the password is invalid
     */
    public User createUser(User user, String password, String repeatPassword) throws IOException, APIServerSideException, APIUnauthorizedException, APINotFoundException, APIInvalidUserException {

        URL url = new URL(Endpoints.USERS.getEndpoint().replace("{IP}", IP));

        JSONObject json = new JSONObject();

        json.put("username", user.getUsernameCreation());
        json.put("password", password);
        json.put("passwordRepeat", repeatPassword);
        json.put("enabled", user.isEnabledCreation());
        json.put("isAdmin", user.isAdminCreation());
        json.put("hasAccessToAllServers", user.isHasAccessToAllServersCreation());
        if (user.getPermissionsCreation() != null) {

            //Here the fun starts
            //Create the object for the permissions. It will later be added to the main json object
            JSONObject permissions = new JSONObject();

            //For each server that has custom permissions
            for (Map.Entry<String, ArrayList<UserPermissions>> entry : user.getPermissionsCreation().entrySet()) {

                //Create the object for that specific server
                JSONObject serverPermissions = new JSONObject();

                //Set the correct permissions
                serverPermissions.put("viewStats", entry.getValue().contains(UserPermissions.VIEW_STATS));
                serverPermissions.put("viewConsole", entry.getValue().contains(UserPermissions.VIEW_CONSOLE));
                serverPermissions.put("useConsole", entry.getValue().contains(UserPermissions.USE_CONSOLE));
                serverPermissions.put("useServerActions", entry.getValue().contains(UserPermissions.USE_SERVER_ACTIONS));

                //Add the server to the permissions object
                permissions.put(entry.getKey(), serverPermissions);
            }

            //Finally add the permissions object to the main json object
            json.put("customServerPermissions", permissions);
        }
        //Finally with the JSON ready, we can now create the connection and actually create the user
        HttpURLConnection conn = createPostConnection(url);

        conn.connect();
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

        writer.write(json.toString());
        writer.flush();

        int responseCode = conn.getResponseCode();

        switch (responseCode) {
            case 400 -> throw new APIInvalidUserException(Errors.INVALID_USER_DETAILS.getMessage());
            case 401 -> throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 403 -> throw new APIUnauthorizedException(Errors.NOT_ADMIN.getMessage());
            case 500 -> throw new APIServerSideException(Errors.API_ERROR.getMessage());
            case 201 -> {
                //Get response
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JSONObject response = new JSONObject(new JSONTokener(reader));

                //Close connection
                conn.disconnect();

                //Return the created user
                return new User(this, response.getString("userId"));
            }
            default -> {
                //Close connection
                conn.disconnect();
                throw new APIServerSideException(Errors.API_ERROR.getMessage());
            }
        }

    }

    /**
     * Execute an action on multiple servers.
     * @param action The action to execute
     * @param servers A list (or array) of servers to execute the action on.
     * @return null if the action is executed successfully on all servers, otherwise a HashMap with the server GUID as key and the response code as value.
     * @throws APIUnauthorizedException if the APIKey is invalid
     * @throws IOException if there is an error with the connection
     * @throws APINoServerAccessException If the API key doesn't have access to all the servers specified
     * @throws APINotFoundException If the all the servers aren't found
     */
    public HashMap<String, Integer> ExecuteMassServerAction(ServerAction action, Server... servers) throws APIUnauthorizedException, IOException, APINoServerAccessException, APINotFoundException {

        if (action == ServerAction.INVALID) {
            throw new IllegalArgumentException(Errors.INVALID_ACTION.getMessage());
        } else if (servers.length == 0) {
            throw new IllegalArgumentException(Errors.NO_SERVERS.getMessage());
        }

        URL url = new URL(Endpoints.MASS_EXECUTE_ACTION.getEndpoint().replace("{IP}", IP));

        HttpURLConnection conn = createPostConnection(url);

        JSONObject json = new JSONObject();
        JSONArray serverArray = new JSONArray();
        for (Server server : servers) {
            serverArray.put(server.getGUID());
        }
        json.put("serverIds", serverArray);
        json.put("action", action.getValue());

        conn.connect();

        String jsonString = json.toString();
        OutputStream os = conn.getOutputStream();

        os.write(jsonString.getBytes());

        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();

        switch (responseCode) {
            case 401:
                throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 403:
                throw new APINoServerAccessException(Errors.NO_SERVER_ACCESS.getMessage());
            case 404:
                throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 500:
                throw new IOException(Errors.API_ERROR.getMessage());
            case 200:
                return null;
            case 207:
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JSONObject response = new JSONObject(new JSONTokener(reader));
                HashMap<String, Integer> map = new HashMap<>();
                JSONArray array = response.getJSONArray("responses");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    map.put(obj.getString("serverId"), obj.getInt("status"));
                }
                return map;
        }

        //Closing everything
        conn.disconnect();

        return null;

    }

    /**
     * Execute a list of commands on multiple servers.
     * @param commands A list (or array) of commands to execute
     * @param servers A list (or array) of servers to execute the commands on.
     * @return null if the commands are executed successfully on all servers, otherwise a HashMap with the server GUID as key and the response code as value.
     * @throws IOException if there is an error with the connection
     * @throws APIUnauthorizedException if the APIKey is invalid
     * @throws APINoServerAccessException If the API key doesn't have access to all the servers specified
     * @throws APINotFoundException If the all the servers aren't found
     */
    public HashMap<String, Integer> ExecuteMassCommands(String[] commands, Server... servers) throws IOException, APIUnauthorizedException, APINoServerAccessException, APINotFoundException {

        if (commands.length == 0) {
            throw new IllegalArgumentException(Errors.NO_COMMANDS.getMessage());
        } else if (servers.length == 0) {
            throw new IllegalArgumentException(Errors.NO_SERVERS.getMessage());
        }

        URL url = new URL(Endpoints.MASS_EXECUTE_COMMANDS.getEndpoint().replace("{IP}", IP));

        HttpURLConnection conn = createPostConnection(url);

        JSONObject json = new JSONObject();
        JSONArray serverArray = new JSONArray();
        for (Server server : servers) {
            serverArray.put(server.getGUID());
        }
        json.put("serverIds", serverArray);
        JSONArray commandArray = new JSONArray();
        for (String command : commands) {
            commandArray.put(command);
        }
        json.put("commands", commandArray);

        conn.connect();

        String jsonString = json.toString();
        OutputStream os = conn.getOutputStream();

        os.write(jsonString.getBytes());

        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();

        switch (responseCode) {
            case 401 -> throw new APIUnauthorizedException(Errors.UNAUTHORIZED.getMessage());
            case 403 -> throw new APINoServerAccessException(Errors.NO_SERVER_ACCESS.getMessage());
            case 404 -> throw new APINotFoundException(Errors.NOT_FOUND.getMessage());
            case 500 -> throw new IOException(Errors.API_ERROR.getMessage());
            case 200 -> {return null;}
            case 207 -> {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JSONObject response = new JSONObject(new JSONTokener(reader));
                HashMap<String, Integer> map = new HashMap<>();
                JSONArray array = response.getJSONArray("responses");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    map.put(obj.getString("serverId"), obj.getInt("status"));
                }
                return map;
            }
        }

        conn.disconnect();

        return null;
    }


    private HttpURLConnection createGetConnection(URL url) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        return conn;
    }

    private HttpURLConnection createPostConnection(URL url) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
        conn.setReadTimeout(5000);
        conn.setRequestProperty("APIKey", token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        return conn;
    }
}
