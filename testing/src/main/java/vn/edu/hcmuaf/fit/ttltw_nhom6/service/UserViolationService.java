//package vn.edu.hcmuaf.fit.ttltw_nhom6.service;
//
//public class UserViolationService {
//    private static UserViolationService instance;
//
//    public static UserViolationService getInstance() {
//        if (instance == null) {
//            instance = new UserViolationService();
//        }
//        return instance;
//    }
//
//    /**
//     * Check nội dung user (chat, comment, review...)
//     */
//    public void checkBadContent(int userId, String username, String content) {
//        if (containsBadWords(content)) {
//            NotificationService.getInstance()
//                    .notifyAdminUserViolation(
//                            userId,
//                            username,
//                            "RULE_BAD_CONTENT",
//                            "Nội dung chứa từ ngữ không phù hợp"
//                    );
//        }
//    }
//
//    /**
//     * Check spam
//     */
//    public void checkSpam(int userId, String username, int actionCount) {
//        if (actionCount > 10) {
//            NotificationService.getInstance()
//                    .notifyAdminUserViolation(
//                            userId,
//                            username,
//                            "RULE_SPAM",
//                            "Spam quá số lần cho phép trong thời gian ngắn"
//                    );
//        }
//    }
//
//    private boolean containsBadWords(String content) {
//        String[] bannedWords = {"xxx", "chửi", "spam"};
//        for (String w : bannedWords) {
//            if (content.toLowerCase().contains(w)) return true;
//        }
//        return false;
//    }
//
//}
