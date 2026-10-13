package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

public class FlashSale_Comics {
    private int flashsaleId;
    private int comicId;

    // Constructor
    public FlashSale_Comics() {}

    public FlashSale_Comics(int flashsaleId, int comicId) {
        this.flashsaleId = flashsaleId;
        this.comicId = comicId;
    }

    // Getters và Setters
    public int getFlashsaleId() {
        return flashsaleId;
    }

    public void setFlashsaleId(int flashsaleId) {
        this.flashsaleId = flashsaleId;
    }

    public int getComicId() {
        return comicId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    @Override
    public String toString() {
        return "FlashSale_Comics{" +
                "flashsaleId=" + flashsaleId +
                ", comicId=" + comicId +
                '}';
    }
}