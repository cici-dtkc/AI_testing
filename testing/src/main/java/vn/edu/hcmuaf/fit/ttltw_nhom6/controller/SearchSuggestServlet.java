package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/search-suggest")
public class SearchSuggestServlet extends HttpServlet {
    private ComicDAO comicDAO;
    private static final int MAX_SUGGESTIONS = 8;

    @Override
    public void init() throws ServletException {
        comicDAO = new ComicDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "max-age=30");

        String keyword = request.getParameter("q");
        PrintWriter out = response.getWriter();

        if (keyword == null || keyword.trim().isEmpty()) {
            out.print("[]");
            return;
        }

        keyword = keyword.trim();

        try {
            List<Comic> suggestions = comicDAO.smartSearchWithFlashSale(keyword);

            int limit = Math.min(suggestions.size(), MAX_SUGGESTIONS);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < limit; i++) {
                Comic c = suggestions.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"id\":").append(c.getId()).append(",");
                json.append("\"name\":").append(toJson(c.getNameComics()));
                json.append("}");
            }
            json.append("]");

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    private String toJson(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}