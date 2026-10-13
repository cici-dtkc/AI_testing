package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleComicsDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;

import java.util.List;
import java.util.Map;

public class FlashSaleService {
    private final FlashSaleDAO flashSaleDAO = new FlashSaleDAO();
    private final FlashSaleComicsDAO flashSaleComicsDAO = new FlashSaleComicsDAO();

    /**
     * Lấy danh sách comics ĐỘC QUYỀN cho một Flash Sale cụ thể
     */
    public List<Map<String, Object>> getExclusiveComicsForFlashSale(int flashSaleId) {
        // Sử dụng query đã tối ưu ở DAO
        return flashSaleComicsDAO.getExclusiveComicsForFlashSale(flashSaleId);
    }


}