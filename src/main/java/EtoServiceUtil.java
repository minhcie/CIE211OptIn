package src.main.java;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.log4j.Logger;

public class EtoServiceUtil {
    private static final Logger log = Logger.getLogger(EtoServiceUtil.class.getName());

    public static EtoAuthentication authenticate() throws Exception {
        EtoAuthentication auth = null;

        // Create REST client to access web services.
        Client client = Client.create();

        // Retrieve SSO authentication token.
        log.info("Retrieving SSO authentication token...\n");
        WebResource webResource = client.resource("https://services.etosoftware.com/API/Security.svc/SSOAuthenticate/");
        String credential = "{\"security\":{\"Email\":\"minh@ciesandiego.org\",\"Password\":\"m1nh@c13\"}}";

        ClientResponse response = webResource.type(MediaType.valueOf("application/json"))
                                             .post(ClientResponse.class, credential);
        if (response.getStatus() != 200) {
            log.error("Authentication failed: HTTP error code: " +
                      response.getStatus() + ", " + response.toString());
            return auth;
        }

        String resp = response.getEntity(String.class);
        String token = parseAuthResponse(resp);
        if (token == null) {
            log.error("Failed to get SSO authentication token");
            return auth; // No SSO authentication token.
        }

        // Retrieve the enterprise key.
        log.info("Retrieving the enteprise key...\n");
        webResource = client.resource("https://services.etosoftware.com/API/Security.svc/GetSSOEnterprises/" + token);
        response = webResource.type(MediaType.valueOf("application/json"))
                              .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            log.error("Failed to get the enteprise key: HTTP error code: " +
                      response.getStatus() + ", " + response.toString());
            return auth;
        }

        resp = response.getEntity(String.class);
        String key = parseEnterpriseKey(resp);
        if (key == null) {
            log.error("Failed to get the enteprise key");
            return auth; // No enterprise key found.
        }

        // Sign on to the site (ID = 91).
        log.info("Signing on to 211 San Diego...");
        webResource = client.resource("https://services.etosoftware.com/API/Security.svc/SSOSiteLogin/91/" + key + "/" + token + "/" + 8);
        response = webResource.type(MediaType.valueOf("application/json"))
                              .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            log.error("Failed to sign on site: HTTP error code: " +
                      response.getStatus() + ", " + response.toString());
            return auth;
        }

        resp = response.getEntity(String.class);
        String securityToken = parseSecurityToken(resp);
        if (securityToken == null) {
            log.error("Failed to get security token");
            return auth; // No security token.
        }

        // All good, save enterprise key and security token.
        auth = new EtoAuthentication(key, securityToken);
        return auth;
    }

    public static void setCurrentProgram(int programId, EtoAuthentication auth) {
        log.info("Set session's current program = " + programId + "\n");

        Client client = Client.create();
        WebResource webResource = client.resource("https://services.etosoftware.com/API/Security.svc/UpdateCurrentProgram/");
        String program = "{\"ProgramID\":" + programId + "}";
        ClientResponse response = webResource.type(MediaType.valueOf("application/json"))
                                             .header("enterpriseGuid", auth.key)
                                             .header("securityToken", auth.securityToken)
                                             .post(ClientResponse.class, program);
        if (response.getStatus() != 200) {
            log.error("Failed to set session's current program: HTTP error code: " +
                      response.getStatus() + ", " + response.toString());
        }

        // @debug.
        //String resp = response.getEntity(String.class);
        //log.info("Response from server:");
        //log.info(resp + "\n");
    }


    public static ClientResponse postRequest(String uri, EtoAuthentication auth,
                                             String inputStr) throws Exception {
        Client client = Client.create();
        WebResource webResource = client.resource(uri);
        ClientResponse response = webResource.type(MediaType.valueOf("application/json"))
                                  .header("enterpriseGuid", auth.key)
                                  .header("securityToken", auth.securityToken)
                                  .post(ClientResponse.class, inputStr);
        return response;
    }

    public static ClientResponse deleteRequest(String uri, EtoAuthentication auth,
                                               String inputStr) throws Exception {
        Client client = Client.create();
        WebResource webResource = client.resource(uri);
        ClientResponse response = webResource.type(MediaType.valueOf("application/json"))
                                  .header("enterpriseGuid", auth.key)
                                  .header("securityToken", auth.securityToken)
                                  .delete(ClientResponse.class, inputStr);
        return response;
    }

    public static Long parseResponse(ClientResponse response, String resultString,
                                     String resultKey) throws Exception {
        Long responseId = null;
        String resp = response.getEntity(String.class);

        /*
        // @debug.
        if (resp.contains("AddTouchPointResponse")) {
            log.info("Response from server:");
            log.info(resp + "\n");
        }
        */

        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject)parser.parse(resp);
        JSONObject result = (JSONObject)jsonObj.get(resultString);
        if (result != null) {
            responseId = (Long)result.get(resultKey);
        }
        return responseId;
    }

    /**
     * Private helper methods.
     */
    private static String parseAuthResponse(String resp) {
        String token = null;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject jsonObj = (JSONObject)obj;

            JSONObject result = (JSONObject)jsonObj.get("SSOAuthenticateResult");
            log.info(result.toString() + "\n");
            long status = (Long)result.get("AuthStatusCode");
            log.info("status: " + status);
            token = (String)result.get("SSOAuthToken");
            log.info("token: " + token + "\n");
        }
        catch (ParseException pe) {
            pe.printStackTrace();
        }
        return token;
    }

    private static String parseEnterpriseKey(String resp) {
        String key = null;
        try {
            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray)parser.parse(resp);

            JSONObject result = (JSONObject)arr.get(0);
            key = (String)result.get("Key");
            log.info("Enterprise Key: " + key);
            String value = (String)result.get("Value");
            log.info("Enterprise Value: " + value + "\n");
        }
        catch (ParseException pe) {
            pe.printStackTrace();
        }
        return key;
    }

    private static String parseSecurityToken(String resp) {
        String token = null;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            token = (String)obj;
            log.info("Security token: " + token + "\n");
        }
        catch (ParseException pe) {
            pe.printStackTrace();
        }
        return token;
    }
}
