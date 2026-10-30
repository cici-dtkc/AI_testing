"""Map a Java source path to the SRS module code (M01-M21)."""
from __future__ import annotations

import re

RULES: list[tuple[str, str]] = [
    (r"admin/profile", "M21"),
    (r"admin/voucher|Voucher", "M09"),
    (r"admin/(ExportExcel|Report)", "M20"),
    (r"admin/DashBoard|Statistics", "M19"),
    (r"admin/(UserManagement|UserPermission|Permission|Role)", "M18"),
    (r"admin/(AdminOrderManagement|Order)", "M17"),
    (r"admin/(AdminInventory|Inventory)", "M16"),
    (r"admin/(AddSeries|EditSeries|DeleteSeries|ListSeries|SeriesManagement|AddCategory|EditCategory"
     r"|UpdateCategory|DeleteCategory|CategoryManagement|ListCategories|SearchCategory|GetCategoryById)", "M15"),
    (r"admin/(AddComic|UpdateComic|DeleteComic|ListComics|GetComic|ProductManagement|AdminProductManagement"
     r"|AdminComicSearch|ToggleComicHidden|CreateFlashSale|ManageFlashSale)", "M14"),
    (r"recommend", "M13"),
    (r"(Notification|FCM|ServiceWorker)", "M12"),
    (r"(Profile|PointTransaction|PointHistory|ShippingAddress|UpdateUser|ChangePassword|UploadAvatar)", "M11"),
    (r"(Review|Return)", "M10"),
    (r"(Voucher|FlashSale)", "M03"),
    (r"(vnpay|VNPay|momo|MoMo|QRPayment|Payment)", "M07"),
    (r"(ghn|GHN|Shipping)", "M08"),
    (r"Cart", "M04"),
    (r"Wishlist", "M05"),
    (r"(Checkout|Order|BuyAgain)", "M06"),
    (r"(Login|Register|Authen|Password|Otp|Logout|Facebook|Google|Session)", "M01"),
    (r"(Comic|Category|Categories|Series|Author|Publisher|Banner|FlashSaleComics|Search|Home|Header"
     r"|FilterComic)", "M02"),
]

DEFAULT = "M02"


def module_of(path: str) -> str:
    p = path.replace("\\", "/")
    for pattern, module in RULES:
        if re.search(pattern, p):
            return module
    return DEFAULT
