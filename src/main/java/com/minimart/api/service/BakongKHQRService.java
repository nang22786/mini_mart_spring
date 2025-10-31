package com.minimart.api.service;

import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BakongKHQRService {

    @Value("${bakong.merchant.account-id}")
    private String merchantAccountId;

    @Value("${bakong.merchant.name}")
    private String merchantName;

    @Value("${bakong.merchant.phone}")
    private String merchantPhone;

    @Value("${bakong.merchant.city}")
    private String merchantCity;

    /**
     * Generate KHQR for payment
     */
    public KHQRGenerationResult generateKHQR(String orderId, BigDecimal amount) {
        try {
            // ✅ ADD VALIDATION
            System.out.println("🔄 Generating KHQR for Order: " + orderId);
            System.out.println("   Merchant Account ID: " + merchantAccountId);
            System.out.println("   Merchant Name: " + merchantName);
            System.out.println("   Merchant Phone: " + merchantPhone);
            System.out.println("   Merchant City: " + merchantCity);
            System.out.println("   Amount: " + amount);

            if (merchantAccountId == null || merchantAccountId.isEmpty()) {
                System.err.println("❌ merchantAccountId is NULL or empty!");
                return new KHQRGenerationResult(false, null, null, "Merchant account ID not configured");
            }

            if (merchantPhone == null || merchantPhone.isEmpty()) {
                System.err.println("❌ merchantPhone is NULL or empty!");
                return new KHQRGenerationResult(false, null, null, "Merchant phone not configured");
            }

            IndividualInfo individualInfo = new IndividualInfo();
            individualInfo.setBakongAccountId(merchantAccountId);
            individualInfo.setAccountInformation(merchantPhone);
            individualInfo.setMerchantName(merchantName);
            individualInfo.setMerchantCity(merchantCity);
            individualInfo.setCurrency(KHQRCurrency.USD);
            individualInfo.setAmount(amount.doubleValue());
            individualInfo.setBillNumber(orderId);
            individualInfo.setStoreLabel("Mini Mart");

            System.out.println("📞 Calling BakongKHQR.generateIndividual()...");
            KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(individualInfo);

            System.out.println("📨 Bakong Response Code: " + response.getKHQRStatus().getCode());
            System.out.println("📨 Bakong Response Message: " + response.getKHQRStatus().getMessage());

            if (response.getKHQRStatus().getCode() == 0) {
                System.out.println("✅ KHQR Generated Successfully!");
                System.out.println("   MD5: " + response.getData().getMd5());
                System.out.println("   QR Length: " + response.getData().getQr().length());

                return new KHQRGenerationResult(
                        true,
                        response.getData().getQr(),
                        response.getData().getMd5(),
                        null);
            } else {
                System.err.println("❌ KHQR Generation Failed!");
                System.err.println("   Code: " + response.getKHQRStatus().getCode());
                System.err.println("   Message: " + response.getKHQRStatus().getMessage());

                return new KHQRGenerationResult(false, null, null,
                        response.getKHQRStatus().getMessage());
            }

        } catch (Exception e) {
            System.err.println("❌ Exception generating KHQR: " + e.getMessage());
            e.printStackTrace();
            return new KHQRGenerationResult(false, null, null, e.getMessage());
        }
    }

    // Result class
    public static class KHQRGenerationResult {
        private final boolean success;
        private final String qrCode;
        private final String md5;
        private final String error;

        public KHQRGenerationResult(boolean success, String qrCode, String md5, String error) {
            this.success = success;
            this.qrCode = qrCode;
            this.md5 = md5;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getQrCode() {
            return qrCode;
        }

        public String getMd5() {
            return md5;
        }

        public String getError() {
            return error;
        }
    }
}