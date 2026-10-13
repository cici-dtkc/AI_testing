package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Series;

import java.io.IOException;
import java.util.*;

@WebServlet("/series-detail")
public class SeriesDetailServlet extends HttpServlet {
    private SeriesDAO seriesDAO;
    private ComicDAO comicDAO;
    private FlashSaleDAO flashSaleDAO;

    @Override
    public void init() throws ServletException {
        seriesDAO = new SeriesDAO();
        comicDAO = new ComicDAO();
        flashSaleDAO = new FlashSaleDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        flashSaleDAO.updateStatuses();

        try {
            String seriesIdParam = request.getParameter("id");

            if (seriesIdParam == null || seriesIdParam.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/");
                return;
            }
            int seriesId = Integer.parseInt(seriesIdParam);

            Optional<Series> seriesOptional = seriesDAO.getSeriesById(seriesId);
            if (!seriesOptional.isPresent()) {
                request.getSession().setAttribute("errorMsg", "Không tìm thấy series");
                response.sendRedirect(request.getContextPath() + "/");
                return;
            }

            Series series = seriesOptional.get();
            if (series.isHidden()) {
                request.getSession().setAttribute("errorMsg", "Series này hiện không khả dụng");
                response.sendRedirect(request.getContextPath() + "/");
                return;
            }

            List<Comic> comicsInSeries = comicDAO.getComicsBySeriesIdWithFlashSale(seriesId);

            Map<Integer, Integer> soldMap = comicDAO.getTotalSoldBySeriesId(seriesId);

            for (Comic comic : comicsInSeries) {
                int totalSold = soldMap.getOrDefault(comic.getId(), 0);
                comic.setTotalSold(totalSold);
            }

            Set<String> authorSet = new HashSet<>();
            Set<String> publisherSet = new HashSet<>();

            for (Comic comic : comicsInSeries) {
                if (comic.getAuthor() != null && !comic.getAuthor().trim().isEmpty()) {
                    String[] authors = comic.getAuthor().split("[,;]");
                    for (String author : authors) {
                        String trimmed = author.trim();
                        if (!trimmed.isEmpty()) {
                            authorSet.add(trimmed);
                        }
                    }
                }

                if (comic.getPublisher() != null && !comic.getPublisher().trim().isEmpty()) {
                    String[] publishers = comic.getPublisher().split("[,;]");
                    for (String publisher : publishers) {
                        String trimmed = publisher.trim();
                        if (!trimmed.isEmpty()) {
                            publisherSet.add(trimmed);
                        }
                    }
                }
            }

            String authors = authorSet.isEmpty() ? null : String.join(", ", authorSet);
            String publishers = publisherSet.isEmpty() ? null : String.join(", ", publisherSet);

            for (Comic comic : comicsInSeries) {
                if (comic.isHasFlashSale()) {
                    System.out.println("Flash Sale: " + comic.getFlashSaleName());
                }
                System.out.println("Normal Price: " + comic.getPrice());
            }

            request.setAttribute("series", series);
            request.setAttribute("comicsInSeries", comicsInSeries);
            request.setAttribute("totalComics", comicsInSeries.size());
            request.setAttribute("seriesAuthors", authors);
            request.setAttribute("seriesPublishers", publishers);

            RequestDispatcher dispatcher = request.getRequestDispatcher("/frontend/public/SeriComic.jsp");
            dispatcher.forward(request, response);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            request.getSession().setAttribute("errorMsg", "ID series không hợp lệ");
            response.sendRedirect(request.getContextPath() + "/");
        } catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("errorMsg", "Có lỗi xảy ra khi tải thông tin series");
            response.sendRedirect(request.getContextPath() + "/");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }
}