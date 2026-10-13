package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ValidationResult;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ValidationUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@WebServlet("/updateUser")
public class UpdateUserServlet extends HttpServlet {
    private UserDao userDao;

    @Override
    public void init() {
        userDao = new UserDao(JdbiConnector.get());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            String ho = request.getParameter("ho");
            String ten = request.getParameter("ten");
            String phone = request.getParameter("phone");
            String email = request.getParameter("email");
            String gender = request.getParameter("gender");

            int day = Integer.parseInt(request.getParameter("day"));
            int month = Integer.parseInt(request.getParameter("month"));
            int year = Integer.parseInt(request.getParameter("year"));
            
            LocalDate birthdate = LocalDate.of(year, month, day);

            String province = request.getParameter("province");
            String district = request.getParameter("district");
            String houseNumber = request.getParameter("house-number");
            ValidationResult result = ValidationUtils.validateUpdateProfile(
                    ho, ten, phone, email, gender,
                    day, month, year,
                    houseNumber, district, province
            );
            if (result.hasErrors()){
                request.setAttribute("errorMessage", result.getErrors());
                request.setAttribute("firstError", result.getFirstError());
                doGet(request, response);
                return;
            }

            String fullName = ho + " " + ten;

            String address = houseNumber;
            if (district != null && !district.trim().isEmpty()) {
                address += ", " + district.trim();
            }
            if (province != null && !province.trim().isEmpty()) {
                address += ", " + province.trim();
            }
            address += ", Việt Nam";

            currentUser.setFullName(fullName);
            currentUser.setPhone(phone);
            currentUser.setEmail(email);
            currentUser.setGender(gender);
            currentUser.setBirthdate(birthdate);
            currentUser.setAddress(address);
            currentUser.setUpdatedAt(LocalDateTime.now());

            boolean success = userDao.updateUser(currentUser);

            if (success) {
                request.getSession().setAttribute("currentUser", currentUser);
                request.setAttribute("message", "Cập nhật thông tin thành công!");
            } else {
                request.setAttribute("message", "Cập nhật thất bại, vui lòng thử lại.");
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
            request.setAttribute("message", "Dữ liệu ngày sinh không hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Kiểm tra đăng nhập
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }


        String fullName = currentUser.getFullName() != null ? currentUser.getFullName().trim() : "";

        String ho = "";
        String ten = "";

        int lastSpace = fullName.lastIndexOf(" ");
        if (lastSpace > 0) {
            ho = fullName.substring(0, lastSpace);
            ten = fullName.substring(lastSpace + 1);
        } else {
            ten = fullName;
        }

        String phone = currentUser.getPhone() != null ? currentUser.getPhone() : "";
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        String gender = currentUser.getGender() != null ? currentUser.getGender() : "male";

        int day = 1;
        int month = 1;
        int year = 2000;

        if (currentUser.getBirthdate() != null) {
                day = currentUser.getBirthdate().getDayOfMonth();
                month = currentUser.getBirthdate().getMonthValue();
                year = currentUser.getBirthdate().getYear();
        }

        String address = currentUser.getAddress() != null ? currentUser.getAddress(): "";

        String houseNumber = "";
        String district = "";
        String province = "";
        String country = "Việt Nam";

        String[] addressParts = address.split(",\\s*");

        if (addressParts.length > 0) houseNumber = addressParts[0].trim();
        if (addressParts.length > 1) district = addressParts[1].trim();
        if (addressParts.length > 2) province = addressParts[2].trim();
        if (addressParts.length > 3) country = addressParts[3].trim();

        request.setAttribute("ho", ho);
        request.setAttribute("ten", ten);
        request.setAttribute("phone", phone);
        request.setAttribute("email", email);
        request.setAttribute("gender", gender);

        request.setAttribute("day", day);
        request.setAttribute("month", month);
        request.setAttribute("year", year);

        request.setAttribute("houseNumber", houseNumber);
        request.setAttribute("district", district);
        request.setAttribute("province", province);
        request.setAttribute("country", country);
        request.setAttribute("isProvinceCode", province.matches("\\\\d+"));
        request.setAttribute("isDistrictCode", district.matches("\\\\d+"));



        // Forward sang trang profile
        request.getRequestDispatcher("/frontend/nguoiB/profile-info.jsp").forward(request, response);
    }
}