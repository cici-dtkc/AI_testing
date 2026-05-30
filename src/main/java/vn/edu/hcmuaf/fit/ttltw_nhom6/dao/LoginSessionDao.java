package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.LoginSession;


import java.util.List;

public class LoginSessionDao {
    private final Jdbi jdbi;

    public LoginSessionDao() {
        this.jdbi = JdbiConnector.get();;
    }

    public List<LoginSession> displayAllStatusLogin(int user_id) {
        String sql = "select ip_address, user_agent, login_time, active  from login_sessions where user_id = :user_id";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("user_id", user_id)
                        .mapToBean(LoginSession.class)
                        .list()
        );
    }
}
