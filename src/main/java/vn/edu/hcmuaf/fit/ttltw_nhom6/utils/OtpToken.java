package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import java.io.Serializable;

public class OtpToken implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String email;
    private final long expiryTime;

    public OtpToken(String code, String email, long expiryTime) {
        this.code = code;
        this.email = email;
        this.expiryTime = expiryTime;
    }

    public String getCode() {
        return code;
    }

    public String getEmail() {
        return email;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public boolean isValid(String inputCode, String inputEmail) {
        return !isExpired() 
                && this.code != null 
                && this.code.equals(inputCode) 
                && this.email != null 
                && this.email.equalsIgnoreCase(inputEmail);
    }
}
