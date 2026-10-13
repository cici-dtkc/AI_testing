package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;
// tạo ket noi de cac thang nho hon ke thua

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;

public abstract class ADao {
    protected Jdbi jdbi = JdbiConnector.get();
}

