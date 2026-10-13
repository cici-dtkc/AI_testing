package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.listener;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CartDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
@WebListener
public class GuestCartSessionListener implements  HttpSessionListener{
    @Override
    public void sessionCreated(HttpSessionEvent se) {

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
//        HttpSession session = se.getSession();
//        User currentUser = (User) session.getAttribute("currentUser");
//        if(currentUser != null) {
//            return;
//        }
//        String sessionId = session.getId();
//        if(sessionId == null) return;
//        try {
//            CartDAO cartDAO = new CartDAO();
//            int deleted = cartDAO.deleteGuestCartBySessionId(sessionId);
//            System.out.println("[GuestCartSessionListener] Session destroyed: "
//                    + sessionId + " -> deleted " + deleted + " guest cart(s)");
//        } catch (Exception e) {
//            System.err.println("[GuestCartSessionListener] Error: " + e.getMessage());
//        }
    }
}
