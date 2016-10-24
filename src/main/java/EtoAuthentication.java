package src.main.java;

/**
 * Object to store ETO's enterprise key and security token
 * after authenticated and signed on to a site.
 */
public class EtoAuthentication {
    public String key;
    public String securityToken;

    public EtoAuthentication(String key, String securityToken) {
        this.key = key;
        this.securityToken = securityToken;
    }
}
