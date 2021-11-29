package com.oryx.imaging.Utilities;

import java.io.*;
import java.net.*;
import java.util.*;

import com.oryx.imaging.Main;
import com.oryx.imaging.constant.Strings;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class RequestsAsync {

    /**
     *
     * @param args
     * Testing
     */
    public static void main(String[] args) throws IOException {

        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);

        String username = "ahmad";
        String password = "QCC8vv-990";
        loginRequest(username, password);

        getPatientPhotos(manager, -1);

        getImagePath(-1, -1,null);

    }

    private static HttpURLConnection prepareConnection(CookieManager manager, String method, String uri, String queries,
                                                       String contentType) throws IOException {

        CookieStore cookieJar =  manager.getCookieStore();
        List <HttpCookie> cookies = cookieJar.getCookies();

        // build cookies string
        StringBuilder cookiesString = new StringBuilder();
        for (HttpCookie cookie: cookies) {
            cookiesString.append(cookie).append(";");
        }

        queries = queries.replace(" ", "%20");

        String _Trailer = Main.get_trailer();
        String u = "https://" + Main.get_realmName() + ".myoryx" + _Trailer + uri + queries;

        URL url = new URL(u);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Cookie", cookiesString.toString());
        conn.setRequestProperty("Content-Type", contentType);

        return conn;

    }

    /**
     * @param username
     * @param password
     */
    public static Response loginRequest(String username, String password) {

        Main.cookieManager = new CookieManager();
        Main.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(Main.cookieManager);

        // try .com , then .ca otherwise fail
        String _Trailer = Main.get_trailer();
        String uri = "https://" + Main.get_realmName() + ".myoryx" + _Trailer + Strings.AuthLogin;

        String loginJson = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        try {

            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(loginJson.getBytes());
            os.flush();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("POST was successful.");
            } else if (responseCode == 401) {
                System.out.println("Wrong password.");
                return new Response(false, responseCode, "Wrong password.");
            } else {
                System.out.println("Unexpected Error. Error Code : " + responseCode);
                return new Response(false, responseCode, "Unexpected Error.");
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder strBuilder = new StringBuilder();

            String output;
            while ((output = br.readLine()) != null) {
                strBuilder.append(output);
            }
            conn.disconnect();

            JsonReader reader = Json.createReader(new StringReader(strBuilder.toString()));
            JsonObject jsonObject = reader.readObject();
            reader.close();

            boolean success = jsonObject.getBoolean("success");
            
            String message = jsonObject.getString("message");
            System.out.println("Login request body: " + jsonObject.toString());
            return new Response(success, responseCode, message);

        } catch (IOException e) {

            e.printStackTrace();

        }
        return null;
    }

//    public static Response isAuthenticated(CookieManager manager) throws IOException {
//        try {
//            HttpURLConnection conn = prepareConnection(manager, "GET", Strings.isAuthenticated, "", "application" +
//                    "/json");
//
//            int responseCode = conn.getResponseCode();
//            if (responseCode == 200) {
//                System.out.println("User is Authenticated.");
//            } else if (responseCode == 401) {
//                System.out.println("Authentication Failed.");
//                return new Response(false, responseCode, "Authentication Failed.");
//            } else {
//                System.out.println("Unexpected Error. Error Code : " + responseCode);
//                return new Response(false, responseCode, "Unexpected Error.");
//            }
//            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
//            StringBuilder strBuilder = new StringBuilder();
//
//            String output;
//            while ((output = br.readLine()) != null) {
//                strBuilder.append(output);
//            }
//            conn.disconnect();
//            return new Response(true, responseCode, conn.getResponseMessage());
//
//        } catch (IOException e){
//            e.printStackTrace();
//            return new Response(false, 500, e.getMessage());
//        }
//    }

    public static Response getPatientPhotos(CookieManager manager, int patientId) throws IOException {

        HttpURLConnection conn = prepareConnection(manager, "GET", Strings.PatientPhotos + patientId, "","application" +
                "/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            System.out.println("POST was successful.");
        } else if (responseCode == 401) {
            System.out.println("Authentication Failed.");
            return new Response(false, responseCode, "Authentication Failed.");
        } else {
            System.out.println("Unexpected Error. Error Code : " + responseCode);
            return new Response(false, responseCode, "Unexpected Error.");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder strBuilder = new StringBuilder();

        String output;
        while ((output = br.readLine()) != null) {
            strBuilder.append(output);
        }
        conn.disconnect();

        return new Response(true, responseCode, strBuilder.toString());

    }

    public static String getImagePath(int patientId, int id, String version) {

        version = "-7413748265808160685";

        String _Trailer = Main.get_trailer();

        return "https://" + Main.get_realmName() + ".myoryx" + _Trailer + Strings.PatientImageThumbnail + patientId + "/image/" + id;// + "/" + version;

    }

    public static boolean pathToDicom(CookieManager manager, String pathToDicom, int patientId) throws IOException {

        Main.log.info("Trying to upload image for Patient (id): " + patientId + " local path to " +
                "image: " + pathToDicom);

        // get sensor name
        String[] str = pathToDicom.split("-");
        String sensorName = str[str.length - 1].split("\\.")[0].replace(" ", "%20");

        String queries =
                "?Version=" + Main.BUILD_VERSION + "&PatientId=" + patientId + "&SensorName=" + sensorName;

        DataOutputStream dos = null;
        InputStream is = null;

        try {
            HttpURLConnection conn = prepareConnection(manager, "POST", Strings.
                            ImageUploadInstance, queries,
                        "application/octet-stream");

//            if (Math.random() < 0.5) {
//                // should fail
//                int code = connection.getResponseCode();
//                System.out.println("Response code of the object is "+code);
//                if (code == 200) {
//                    System.out.println("OK");
//                } else {
//                    System.out.println("Failed To Upload with Error Code: " + code);
//                }
//            }


            // if this fails we should stop the process
            File dicomFile = new File(pathToDicom);

            dos = new DataOutputStream(conn.getOutputStream());
            is = new FileInputStream(dicomFile);
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = is.read(bytes)) != -1) {
                dos.write(bytes, 0, len);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder strBuilder = new StringBuilder();

            String output;
            while ((output = br.readLine()) != null) {
                strBuilder.append(output);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("POST was successful.");
            } else if (responseCode == 401) {
                System.out.println("Authentication Failed.");
//                return new Response(false, responseCode, "Authentication Failed.");
            } else {
                System.out.println("Unexpected Error. Error Code : " + responseCode);
//                return new Response(false, responseCode, "Unexpected Error.");
            }

            Main.log.info("File Uploaded Successfully for Patient (id): " + patientId + " local path to image: " + pathToDicom);
//            System.out.println("Output of upload " + strBuilder.toString());
            conn.disconnect();
        } catch (ProtocolException p) {
            System.out.println("PROTOCOL Exception happened");
            return false;
        } catch (FileNotFoundException f) {
          // file not find
          f.printStackTrace();
          System.out.println("FILE NOT SAVED CORRECTLY!");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dos != null) dos.close();
            if (is != null) is.close();
        }

        return true;
    }

    public static Response getPatientList(CookieManager manager, String query) throws IOException {
        String queries = "";
        if (query != null ) {
            queries = "?allPatients=false&query=" + query;
        }

        HttpURLConnection connection =  prepareConnection(manager, "GET", Strings.PatientData, queries,
                "application/json");

        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            System.out.println("POST was successful.");
        } else if (responseCode == 401) {
            System.out.println("Wrong password.");
            return new Response(false, responseCode, "Wrong password.");
        } else {
            System.out.println("Unexpected Error. Error Code : " + responseCode);
            return new Response(false, responseCode, "Unexpected Error.");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
        StringBuilder strBuilder = new StringBuilder();

        String output;
        while ((output = br.readLine()) != null) {
            strBuilder.append(output);
        }

        connection.disconnect();
        return new Response(true, responseCode, strBuilder.toString());

    }

    public static boolean isConnected(){
        return canConnectTo("http://www.google.com");
    }

    public static Response canConnectToRealm(String realm) {
        // try to connect to .com first
        String uri = "https://" + realm + ".myoryx";
        if (canConnectTo(uri + ".com")) {
            return new Response(true, 400, "US");
        } else if (canConnectTo(uri + ".ca")) {
            return new Response(true, 400, "Canada");
        } else {
            return new Response(false, 404, "NA");
        }
    }

    public static boolean canConnectTo(String address) {
        try {
            URL url = new URL(address);
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        } catch (IOException e) {
            return false;
        }
        
    }

    public static AuthenticationResponse isAuthenticated(CookieManager manager) throws IOException {

        // Log it
        Main.log.info("RequestsAsync.isAuthenticated called");
        
        System.out.println("GETTING RequestsAsync.isAuthenticated");

        HttpURLConnection conn = prepareConnection(manager, "GET", Strings.Authenticated, "","");

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder strBuilder = new StringBuilder();

        String output;
        while ((output = br.readLine()) != null) {
            strBuilder.append(output);
        }

//        System.out.println(strBuilder.toString());

        JsonReader reader = Json.createReader(new StringReader(strBuilder.toString()));
        JsonObject jsonObject = reader.readObject();
        reader.close();

        boolean success = jsonObject.getBoolean("success");
        String imagingUpdatesToken = "";
        if (success) {
            try {
                imagingUpdatesToken = jsonObject.getString("imagingUpdatesToken");
                
            } catch (NullPointerException e) {
                Main.log.info("imagingUpdatesToken - Not Available");
                System.out.println("imagingUpdatesToken - Not Available");
            }
        }

        AuthenticationResponse authResponse = new AuthenticationResponse();
        authResponse.setMessage("");
        authResponse.setImagingUpdatesToken(imagingUpdatesToken);

//        authResponse.setSuccess(Math.random() > 0.5);
        authResponse.setSuccess(success);

        return authResponse;

    }

    public static void uploadLogFile(String path, String filename, String token) throws IOException {

//        Main.log.info("Uploading log file - name: " + filename);
//        Main.log.info("Uploading log file - token: " + token);

        String contentType = "text/plain";
        String method = "POST";
        String authorization = "Bearer " + token;

        String realm = Main.get_realmName();
        String queries = "?uploadType=media&name=logs/" + realm + "/" + filename;
        String u = "https://storage.googleapis.com/upload/storage/v1/b/evs-ksd-qa-imaging-update/o" + queries;

        URL url = new URL(u);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Authorization", authorization);

        DataOutputStream dos = null;
        InputStream is = null;

        File logFile = new File(path);

        dos = new DataOutputStream(conn.getOutputStream());
        is = new FileInputStream(logFile);
        byte[] bytes = new byte[1024];
        int len = 0;
        while ((len = is.read(bytes)) != -1) {
            dos.write(bytes, 0, len);
        }
//        System.out.println(conn.getContent());
//        System.out.println(conn.getResponseCode());
//        System.out.println(conn.getErrorStream());

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder strBuilder = new StringBuilder();

        String output;
        while ((output = br.readLine()) != null) {
            strBuilder.append(output);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            System.out.println("POST was successful.");
        } else if (responseCode == 401) {
            System.out.println("Authentication Failed.");
//                return new Response(false, responseCode, "Authentication Failed.");
        } else {
            System.out.println("Unexpected Error. Error Code : " + responseCode);
//                return new Response(false, responseCode, "Unexpected Error.");
        }

    }

    public static RemoteVersionResponse getRemoteVersion(String token) throws IOException {

        System.out.println("GETTING REMOTE VERSION");

        String method = "GET";
        String authorization = "Bearer " + token;
        String realm = Main.get_realmName();
//        String queries = "?uploadType=media&name=logs/" + realm + "/" + filename;

        String projectRegion = "";
        if (Main.get_realmName().equals("qa1.qa")) {
            // QA
            projectRegion = "evs-ksd-qa-imaging-update/o/installers%2Fqa1.qa%2Fmysetup.exe";
        } else if (Main.get_trailer().equals(".ca")) {
            // Canada
            projectRegion = "evs-ksd-ca-imaging-update/o/installers%2Fmysetup.exe";
        } else if (Main.get_trailer().equals(".com")) {
            // US
            projectRegion = "evs-production-imaging-update/o/installers%2Fmysetup.exe";
        }

        String u = "https://storage.googleapis.com/storage/v1/b/" + projectRegion;

        URL url = new URL(u);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(method);
//        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Authorization", authorization);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            System.out.println("POST was successful.");
        } else if (responseCode == 401 || responseCode == 403) {
            System.out.println("Authentication Failed. " + responseCode);
            RemoteVersionResponse remoteVersionResponse = new RemoteVersionResponse();
            remoteVersionResponse.setSuccess(false);
            remoteVersionResponse.setResponseCode(conn.getResponseCode());
            remoteVersionResponse.setMessage("Authentication Failed.");
            return remoteVersionResponse;
        } else {
            System.out.println("Unexpected Error. Error Code : " + responseCode);
            RemoteVersionResponse remoteVersionResponse = new RemoteVersionResponse();
            remoteVersionResponse.setSuccess(false);
            remoteVersionResponse.setResponseCode(conn.getResponseCode());
            remoteVersionResponse.setMessage("Unexpected Error.");
            return remoteVersionResponse;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder strBuilder = new StringBuilder();

        String output;
        while ((output = br.readLine()) != null) {
            strBuilder.append(output);
        }

        System.out.println(strBuilder.toString());

        JsonReader reader = Json.createReader(new StringReader(strBuilder.toString()));
        JsonObject jsonObject = reader.readObject();
        reader.close();

//        boolean success = jsonObject.getBoolean("success");
        String imagingUpdatesToken = "";
        try {
            JsonObject metaDataObject = jsonObject.getJsonObject("metadata");
            String fileSize = jsonObject.getString("size");
            String installerVersion = metaDataObject.getString("version");

            System.out.println("version is: " + installerVersion);
            System.out.println("POST was successful.");

            RemoteVersionResponse remoteVersionResponse = new RemoteVersionResponse();
            remoteVersionResponse.setSuccess(true);
            remoteVersionResponse.setResponseCode(conn.getResponseCode());
            remoteVersionResponse.setMessage(installerVersion);
            remoteVersionResponse.setVersion(installerVersion);
            remoteVersionResponse.setSize(fileSize);

            return remoteVersionResponse;

        } catch (NullPointerException e) {
            Main.log.info("imagingUpdatesToken - Not Available");
            return null;
        }
    }

//    public static FileResponse getInstallerFile(String token) throws IOException {
//
//        String method = "GET";
//        String authorization = "Bearer " + token;
//
////        String realm = Main.get_realmName();
//        String u = "https://storage.googleapis.com/download/storage/v1/b/evs-ksd-qa-imaging-update/o/installers%2Fqa1" +
//                ".qa%2Fmysetup.exe?generation=1620388057352097&alt=media";
//
//        URL url = new URL(u);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setDoOutput(true);
//        conn.setRequestMethod(method);
////        conn.setRequestProperty("Content-Type", contentType);
//        conn.setRequestProperty("Authorization", authorization);
//
//        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
//        StringBuilder strBuilder = new StringBuilder();
//
//        String output;
//        while ((output = br.readLine()) != null) {
//            strBuilder.append(output);
//        }
//        JsonReader reader = Json.createReader(new StringReader(strBuilder.toString()));
//        JsonObject jsonObject = reader.readObject();
//
//
//
//        int responseCode = conn.getResponseCode();
//        if (responseCode == 200) {
//            System.out.println("POST was successful.");
//        } else if (responseCode == 401) {
//            System.out.println("Authentication Failed.");
////                return new Response(false, responseCode, "Authentication Failed.");
//        } else {
//            System.out.println("Unexpected Error. Error Code : " + responseCode);
////                return new Response(false, responseCode, "Unexpected Error.");
//        }
//        return new FileResponse();
//    }
}