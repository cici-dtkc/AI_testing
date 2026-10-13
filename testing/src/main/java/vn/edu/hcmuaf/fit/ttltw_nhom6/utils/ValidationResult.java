package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ValidationResult {
    private final Map<String, String> errors = new LinkedHashMap<>();

    public void addError(String field, String message) {
        errors.put(field, message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    //    lay loi 1 truong cu the
    public String getError(String field) {
        return errors.getOrDefault(field, null);
    }

    //    lay loi dau tien (toast, alert)
    public String getFirstError() {
        return errors.values().stream().findFirst().orElse(null);
    }

}
