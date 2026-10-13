package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ReportDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.RevenueRow;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@WebServlet("/admin/export-excel")
public class ExportExcelServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filter = request.getParameter("filter");     // today/week/month/year
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");

        ReportDAO dao = new ReportDAO();
        List<RevenueRow> rows = dao.getRevenueData(filter, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Doanh thu");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0 \"đ\""));

            Row header = sheet.createRow(0);
            String[] columns = {"Thời gian", "Doanh thu (đ)", "Số đơn hàng", "Giá trị TB"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowNum = 1;
            for (RevenueRow r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getLabel());

                Cell revenueCell = row.createCell(1);
                revenueCell.setCellValue(r.getRevenue());
                revenueCell.setCellStyle(currencyStyle);

                row.createCell(2).setCellValue(r.getTotalOrders());

                Cell avgCell = row.createCell(3);
                avgCell.setCellValue(r.getAvgValue());
                avgCell.setCellStyle(currencyStyle);
            }

            Row totalRow = sheet.createRow(rowNum);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TỔNG");
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            totalLabel.setCellStyle(boldStyle);

            Cell totalRevenue = totalRow.createCell(1);
            totalRevenue.setCellFormula("SUM(B2:B" + rowNum + ")");
            totalRevenue.setCellStyle(currencyStyle);

            String filename;
            if ("custom".equals(filter) && startDate != null && endDate != null) {
                filename = "doanhthu_" + startDate + "_" + endDate + ".xlsx";
            } else {
                filename = "doanhthu_" + filter + "_" +
                        new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx";
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            workbook.write(response.getOutputStream());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}