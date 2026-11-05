BASE_URL = "http://localhost:8080/TTLTW_Nhom6"
BROWSER_TYPE = "chromium"
HEADLESS = True
VIEWPORT = {"width": 1280, "height": 720}
TIMEOUT_DEFAULT = 30000
TIMEOUT_NAVIGATION = 60000

# Auth configuration
AUTH = {
    "base_url": "http://localhost:8080/TTLTW_Nhom6",
    "credentials": {
        "admin": {
            "user_env": "COMIC_ADMIN_USER",
            "pass_env": "COMIC_ADMIN_PASS"
        },
        "user": {
            "user_env": "COMIC_USER_USER",
            "pass_env": "COMIC_USER_PASS"
        }
    },
    "role_inference_order": [
        "module_url_map",
        "precondition_keywords",
        "title_keywords",
        "url_pattern",
        "default_none"
    ],
    "admin_keywords": ["admin", "quản trị"],
    "login_keywords": ["đăng nhập", "logged in"],
    "storage_state_dir": "outputs/.auth"
}

SELECTOR_MAP = {
    "tên đăng nhập": "[data-testid='username'], #username, [name='username']",
    "username": "[data-testid='username'], #username, [name='username']",
    "mật khẩu": "[data-testid='password'], #password, [name='password']",
    "password": "[data-testid='password'], #password, [name='password']",
    "email": "[data-testid='email'], #email, [name='email']",
    "số điện thoại": "[data-testid='phone'], #phone, [name='phone']",
    "địa chỉ": "[data-testid='address'], #address, [name='address']",
    "đăng nhập": "[data-testid='login-btn'], button:has-text('Đăng nhập')",
    "đăng ký": "[data-testid='register-btn'], button:has-text('Đăng ký')",
    "thanh toán": "[data-testid='checkout-btn'], button:has-text('Thanh toán')",
    "thêm vào giỏ": "[data-testid='add-to-cart'], button:has-text('Thêm vào giỏ')",
}

BROWSER = {
    "viewport": {"width": 1920, "height": 1080},
    "timeout_step": 5000,
    "timeout_navigation": 30000,
    "timeout_probe": 500,
    "timeout_candidate": 5000,
    "wait_after_nav": {
        "domcontentloaded": True,
        "element": "body",
        "element_timeout": 3000,
    },
}
