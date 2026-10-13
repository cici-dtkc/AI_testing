def test_login():
    assert "admin".upper() == "ADMIN"


def test_cart_total():
    assert sum([10, 20, 30]) == 60