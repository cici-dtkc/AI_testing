package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.WishlistDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "WishlistViewServlet", value = "/wishlist")
public class WishlistViewServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser != null) {
            WishlistDAO wishlistDAO = new WishlistDAO();
            List<Comic> wishlistComics = wishlistDAO.getWishlistComics(currentUser.getId());

            request.setAttribute("wishlistComics", wishlistComics);
            request.setAttribute("wishlistCount", wishlistComics != null ? wishlistComics.size() : 0);
        } else {
            request.setAttribute("wishlistComics", null);
            request.setAttribute("wishlistCount", 0);
        }

        request.getRequestDispatcher("/frontend/nguoiB/wishlist.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}