package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // Hash mật khẩu
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // Kiểm tra mật khẩu nhập vào có khớp với hash không
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    //thêm trường hợp check lại pass cho server khi login,  tránh người dùng có bypass
    public static boolean isValidPasswordFormat(String password) {
        // Ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt
        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password != null && password.matches(regex);
    }

}
