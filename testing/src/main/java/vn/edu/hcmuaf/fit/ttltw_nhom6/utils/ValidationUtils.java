package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import java.time.LocalDate;

public class ValidationUtils {
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";
    private static final String PHONE_REGEX = "^(0[35789])[0-9]{8}$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_]).{8,}$";
    private static final String NAME_REGEX = "^[\\p{L}\\s]+$";
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,30}$";

    public static String normalizePhone(String phone) {
        if (isBlank(phone)) return null;
        String normalized = phone.trim().replaceAll("\\s+", "");
        if (normalized.startsWith("+84")) {
            normalized = "0" + normalized.substring(3);
        }
        return normalized;
    }


    public static boolean isValidPhone(String phone) {
        String normalized = normalizePhone(phone);
        return normalized != null && normalized.matches(PHONE_REGEX);
    }

    public static boolean isValidUsername(String username) {
        return !isBlank(username) && username.matches(USERNAME_REGEX);
    }

    public static boolean isBlank(String... values) {
        for (String v : values)
            if (v == null || v.trim().isEmpty()) return true;
        return false;
    }

    public static boolean isAllBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return false;
        }
        return true;
    }

    public static boolean isAtLeastOne(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return true;
        }
        return false;
    }

    public static boolean isValidEmail(String email) {
        return !isBlank(email) && email.matches(EMAIL_REGEX);
    }

    public static boolean isValidPassword(String password) {
        return !isBlank(password) && password.matches(PASSWORD_REGEX);
    }

    public static boolean isValidName(String name) {
        return !isBlank(name) && name.trim().length() >= 2 && name.matches(NAME_REGEX);
    }

    public static boolean isValidBirthdate(int day, int month, int year) {
        try {
            LocalDate birthdate = LocalDate.of(year, month, day);
            LocalDate today = LocalDate.now();
            int age = today.getYear() - birthdate.getYear();
            return !birthdate.isAfter(today) && age >= 5 && age <= 120;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidAddress(String houseNumber, String district, String province) {
        return !isBlank(houseNumber, district, province);
    }

    public static ValidationResult validateUpdateProfile(String ho, String ten,
                                                         String phone, String email,
                                                         String gender,
                                                         int day, int month, int year,
                                                         String houseNumber, String district, String province) {
        ValidationResult result = new ValidationResult();
        if (!isValidName(ho))
            result.addError("ho", "Họ không hợp lệ (chỉ chứa chữ cái, tối thiểu 2 ký tự)");
        if (!isValidName(ten))
            result.addError("ten", "Tên không hợp lệ (chỉ chứa chữ cái, tối thiểu 2 ký tự)");
        if (!isValidPhone(phone))
            result.addError("phone", "Số điện thoại không hợp lệ (phải bắt đầu 03/05/07/08/09, đủ 10 số)");
        if (!isValidEmail(email))
            result.addError("email", "Email không hợp lệ");
        if (isBlank(gender) || (!gender.equals("male") && !gender.equals("female")))
            result.addError("gender", "Giới tính không hợp lệ");
        if (!isValidBirthdate(day, month, year))
            result.addError("birthdate", "Ngày sinh không hợp lệ");
        if (!isValidAddress(houseNumber, district, province))
            result.addError("address", "Vui lòng điền đầy đủ địa chỉ");
        return result;
    }

    public static ValidationResult validateChangePassword(String oldPassword, String newPassword, String confirmPassword) {
        ValidationResult result = new ValidationResult();
        if (isBlank(oldPassword))
            result.addError("oldPassword", "Vui lòng nhập mật khẩu cũ");

        if (!isValidPassword(newPassword))
            result.addError("newPassword",
                    "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và ký tự đặc biệt");

        if (!newPassword.equals(confirmPassword))
            result.addError("confirmPassword", "Xác nhận mật khẩu không khớp");

        if (!isBlank(oldPassword, newPassword) && oldPassword.equals(newPassword))
            result.addError("newPassword", "Mật khẩu mới không được trùng mật khẩu cũ");

        return result;
    }

    public static ValidationResult validateRegister(String username, String email, String phone, String password, String confirmPassword) {
        ValidationResult result = new ValidationResult();
        if (!isValidUsername(username))
            result.addError("username", "Tên đăng nhập chỉ gồm chữ, số, dấu gạch dưới (3–30 ký tự)");
        boolean emailFilled = !isBlank(email);
        boolean phoneFilled = !isBlank(phone);
        if (!emailFilled && !phoneFilled) {
            result.addError("emailPhone", "Vui lòng nhập ít nhất email hoặc số điện thoại");
        } else {
            if (emailFilled && !isValidEmail(email))
                result.addError("email", "Email không đúng định dạng");
            if (phoneFilled && !isValidPhone(phone))
                result.addError("phone", "Số điện thoại không hợp lệ (VD: 0912345678 hoặc +84912345678)");
        }
        if (!isValidPassword(password))
            result.addError("password",
                    "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và ký tự đặc biệt");
        if (isBlank(confirmPassword) || !confirmPassword.equals(password))
            result.addError("confirmPassword", "Xác nhận mật khẩu không khớp");
        return result;
    }

}
