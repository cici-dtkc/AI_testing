BASE_URL = "http://localhost:8080/TTLTW_Nhom6"
BROWSER_TYPE = "chromium"
HEADLESS = True
VIEWPORT = {"width": 1280, "height": 720}
TIMEOUT_DEFAULT = 30000
TIMEOUT_NAVIGATION = 60000

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
