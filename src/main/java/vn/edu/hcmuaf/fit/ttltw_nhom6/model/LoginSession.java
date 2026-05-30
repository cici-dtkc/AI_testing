package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import java.time.LocalDateTime;

public class LoginSession {
    private int id;

    @ColumnName("user_id")
    private int userId;

    @ColumnName("ip_address")
    private String ipAddress;

    @ColumnName("user_agent")
    private String userAgent;

    @ColumnName("login_time")
    private LocalDateTime loginTime;

    @ColumnName("active")
    private boolean active;

    public LoginSession() {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
