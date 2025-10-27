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
            IndividualInfo individualInfo = new IndividualInfo();
            individualInfo.setBakongAccountId(merchantAccountId);
            individualInfo.setAccountInformation(merchantPhone);
            individualInfo.setMerchantName(merchantName);
            individualInfo.setMerchantCity(merchantCity);
            individualInfo.setCurrency(KHQRCurrency.USD);
            individualInfo.setAmount(amount.doubleValue());
            individualInfo.setBillNumber(orderId);
            individualInfo.setStoreLabel("Mini Mart");
            
            KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(individualInfo);
            
            if (response.getKHQRStatus().getCode() == 0) {
                System.out.println("✅ KHQR Generated - Order #" + orderId);
                System.out.println("   MD5: " + response.getData().getMd5());
                
                return new KHQRGenerationResult(
                    true,
                    response.getData().getQr(),
                    response.getData().getMd5(),
                    null
                );
            } else {
                return new KHQRGenerationResult(false, null, null, 
                    response.getKHQRStatus().getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error generating KHQR: " + e.getMessage());
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
        
        public boolean isSuccess() { return success; }
        public String getQrCode() { return qrCode; }
        public String getMd5() { return md5; }
        public String getError() { return error; }
    }
}