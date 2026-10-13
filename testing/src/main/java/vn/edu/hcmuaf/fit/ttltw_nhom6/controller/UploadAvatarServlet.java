package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.CloudinaryService;

import java.io.IOException;

@WebServlet(name = "UploadAvatarServlet", urlPatterns = {"/upload-avatar"})
@MultipartConfig
public class UploadAvatarServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }
        Part filePart = request.getPart("avatar");
        try {
            String imageUrl = CloudinaryService.uploadImage(filePart, "avatars");
            if (imageUrl != null) {
                UserDao.getInstance().updateAvatar(currentUser.getId(), imageUrl);
                currentUser.setAvatarUrl(imageUrl);
                request.getSession().setAttribute("currentUser", currentUser);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/updateUser");

    }
}
