package com.minimart.api.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OCRService {

    @Value("${google.credentials.path:google-credentials.json}")
    private String credentialsPath;

    private ImageAnnotatorClient visionClient;

    /**
     * Initialize Google Cloud Vision client
     */
    @PostConstruct
    public void init() {
        try {
            String json = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");
            if (json == null || json.isEmpty()) {
                throw new RuntimeException("Google credentials not set in environment variable!");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-vision"));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();

            visionClient = ImageAnnotatorClient.create(settings);

            System.out.println("✅ Google Cloud Vision initialized successfully!");

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Google Cloud Vision", e);
        }
    }

    /**
     * Extract text from image file using Google Cloud Vision
     */
    public String extractText(File imageFile) throws IOException {
        try {
            System.out.println("📄 Reading image with Google Cloud Vision...");
            System.out.println("📁 Image file: " + imageFile.getAbsolutePath());
            System.out.println("📂 File exists: " + imageFile.exists());
            System.out.println("📏 File size: " + imageFile.length() + " bytes");

            // Read image file into bytes
            ByteString imgBytes = ByteString.readFrom(new FileInputStream(imageFile));

            // Build the image
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // Set the feature type to TEXT_DETECTION
            Feature feat = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            // Build the request
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();

            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);

            // Perform the request
            System.out.println("🚀 Sending request to Google Cloud Vision...");
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            // Extract text from response
            StringBuilder extractedText = new StringBuilder();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.err.println("❌ OCR Error: " + res.getError().getMessage());
                    throw new IOException("Google Vision API Error: " + res.getError().getMessage());
                }

                // The first EntityAnnotation contains the entire detected text
                if (res.getTextAnnotationsCount() > 0) {
                    EntityAnnotation annotation = res.getTextAnnotations(0);
                    extractedText.append(annotation.getDescription());
                }
            }

            String text = extractedText.toString().trim();

            if (text.isEmpty()) {
                System.out.println("⚠️ No text detected in image");
                throw new IOException("No text detected in image");
            }

            System.out.println("✅ OCR completed successfully!");
            System.out.println("📝 Extracted text:");
            System.out.println("─────────────────────────────────");
            System.out.println(text);
            System.out.println("─────────────────────────────────");
            System.out.println("📊 Text length: " + text.length() + " characters");

            return text;

        } catch (Exception e) {
            System.err.println("❌ Google Vision OCR failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("OCR failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract amount from text
     * Looks for patterns like: $10.50, USD 10.50, 10.50
     */
    public BigDecimal extractAmount(String text) {
        System.out.println("💰 Searching for amounts in text...");

        // Multiple patterns to catch different formats
        String[] patterns = {
                "(?:USD|\\$|US\\$)\\s*([\\d,]+\\.\\d{2})", // USD 10.50, $10.50
                "([\\d,]+\\.\\d{2})\\s*(?:USD|\\$)", // 10.50 USD, 10.50$
                "(?:Amount|Total|Received|You received)\\s*:?\\s*(?:USD|\\$)?\\s*([\\d,]+\\.\\d{2})", // Amount: 10.50
                "([\\d,]+\\.\\d{2})", // Just 10.50
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replace(",", "");
                    BigDecimal amount = new BigDecimal(amountStr);

                    // Validate amount is reasonable (between $0.01 and $999,999.99)
                    if (amount.compareTo(new BigDecimal("0.01")) >= 0 &&
                            amount.compareTo(new BigDecimal("999999.99")) <= 0) {
                        System.out.println("✅ Found valid amount: $" + amount);
                        return amount;
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                }
            }
        }

        System.out.println("⚠️ No valid amount found in text");
        return null;
    }

    /**
     * Extract reference number from text
     */
    public String extractReference(String text) {
        System.out.println("🔖 Searching for reference in text...");

        // Pattern to match reference like MM7, MM123
        Pattern pattern = Pattern.compile(
                "(?:REF|Reference|Ref\\.|REF:|Ref:|Transaction|Txn)\\s*:?\\s*([A-Z0-9]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String ref = matcher.group(1).toUpperCase();
            System.out.println("✅ Found reference: " + ref);
            return ref;
        }

        System.out.println("⚠️ No reference found in text");
        return null;
    }

    /**
     * Verify payment screenshot
     * Returns true if amount matches
     */
    public boolean verifyPayment(String text, BigDecimal expectedAmount) {
        System.out.println("🔍 Verifying payment...");
        System.out.println("   Expected amount: $" + expectedAmount);

        // Extract amount from OCR text
        BigDecimal extractedAmount = extractAmount(text);

        if (extractedAmount == null) {
            System.out.println("❌ Verification FAILED: No amount found in screenshot");
            return false;
        }

        // Compare amounts
        boolean matches = extractedAmount.compareTo(expectedAmount) == 0;

        if (matches) {
            System.out.println("✅ Verification SUCCESS: Expected $" + expectedAmount + " = Found $" + extractedAmount);
        } else {
            System.out.println("❌ Verification FAILED: Expected $" + expectedAmount + " ≠ Found $" + extractedAmount);
        }

        return matches;
    }

    /**
     * Cleanup resources when application stops
     */
    @PreDestroy
    public void destroy() {
        if (visionClient != null) {
            visionClient.close();
            System.out.println("✅ Google Cloud Vision client closed");
        }
    }

    /**
     * Extract Transaction ID (Tax ID) from text
     * Looks for patterns like:
     * - Tax ID: 43117156234
     * - Trx. ID: 43117156234
     * - Transaction ID: 12345
     * - Standalone numbers (10-15 digits)
     */
    public String extractTransactionId(String text) {
        System.out.println("🔖 Searching for Transaction ID / Tax ID...");

        if (text == null || text.trim().isEmpty()) {
            System.out.println("❌ Text is empty");
            return null;
        }

        // Normalize whitespace
        String normalizedText = text.replaceAll("\\s+", " ");

        // Pattern 1: Tax ID: 43117156234
        Pattern taxIdPattern = Pattern.compile(
                "(?:Tax\\s*ID|TaxID)\\s*:?\\s*([0-9]{10,15})",
                Pattern.CASE_INSENSITIVE);
        Matcher taxIdMatcher = taxIdPattern.matcher(normalizedText);
        if (taxIdMatcher.find()) {
            String taxId = taxIdMatcher.group(1);
            System.out.println("✅ Found Tax ID: " + taxId);
            return taxId;
        }

        // Pattern 2: Trx. ID: 43117156234 or Transaction ID: 12345
        Pattern trxIdPattern = Pattern.compile(
                "(?:Trx\\.?\\s*ID|Transaction\\s*ID|TRX\\s*ID)\\s*:?\\s*([0-9]{10,15})",
                Pattern.CASE_INSENSITIVE);
        Matcher trxIdMatcher = trxIdPattern.matcher(normalizedText);
        if (trxIdMatcher.find()) {
            String trxId = trxIdMatcher.group(1);
            System.out.println("✅ Found Transaction ID: " + trxId);
            return trxId;
        }

        // Pattern 3: Reference: 12345 or Ref: 12345
        Pattern refPattern = Pattern.compile(
                "(?:REF|Reference|Ref\\.)\\s*:?\\s*([A-Z0-9]{8,20})",
                Pattern.CASE_INSENSITIVE);
        Matcher refMatcher = refPattern.matcher(normalizedText);
        if (refMatcher.find()) {
            String ref = refMatcher.group(1);
            System.out.println("✅ Found Reference: " + ref);
            return ref;
        }

        // Pattern 4: Standalone number (10-15 digits, likely transaction ID)
        Pattern standalonePattern = Pattern.compile("\\b([0-9]{10,15})\\b");
        Matcher standaloneMatcher = standalonePattern.matcher(normalizedText);
        if (standaloneMatcher.find()) {
            String standalone = standaloneMatcher.group(1);
            System.out.println("✅ Found standalone ID: " + standalone);
            return standalone;
        }

        // Pattern 5: Alphanumeric codes (for Wing, ACLEDA formats like MM7492746284)
        Pattern alphaNumPattern = Pattern.compile("\\b([A-Z]{2}[0-9]{10,15})\\b");
        Matcher alphaNumMatcher = alphaNumPattern.matcher(normalizedText);
        if (alphaNumMatcher.find()) {
            String alphaNum = alphaNumMatcher.group(1);
            System.out.println("✅ Found alphanumeric ID: " + alphaNum);
            return alphaNum;
        }

        System.out.println("⚠️ No Transaction ID / Tax ID found");
        System.out.println("📝 Full text for debugging:");
        System.out.println(normalizedText);
        return null;
    }

    /**
     * Extract Transaction Date from text
     * Looks for various date formats
     */
    public LocalDateTime extractTransactionDate(String text) {
        System.out.println("📅 Searching for Transaction Date...");

        if (text == null || text.trim().isEmpty()) {
            System.out.println("❌ Text is empty");
            return null;
        }

        try {
            // Pattern 1: Oct 19, 2025 | 4:01PM (with time)
            Pattern pattern1 = Pattern.compile(
                    "([A-Za-z]{3})\\s+(\\d{1,2}),?\\s+(\\d{4})\\s*[|]?\\s*(\\d{1,2}):(\\d{2})\\s*(AM|PM)?",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher1 = pattern1.matcher(text);

            if (matcher1.find()) {
                try {
                    String month = matcher1.group(1);
                    String day = matcher1.group(2);
                    String year = matcher1.group(3);
                    String hour = matcher1.group(4);
                    String minute = matcher1.group(5);
                    String ampm = matcher1.group(6) != null ? matcher1.group(6) : "AM";

                    String dateStr = month + " " + day + ", " + year + " " + hour + ":" + minute + ampm;
                    System.out.println("✅ Found date string (with time): " + dateStr);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mma", Locale.ENGLISH);
                    LocalDateTime transactionDate = LocalDateTime.parse(dateStr.replace(" ", ""), formatter);

                    System.out.println("✅ Parsed Transaction Date: " + transactionDate);
                    return transactionDate;
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to parse pattern 1: " + e.getMessage());
                }
            }

            // Pattern 2: Oct 19, 2025 (date only, no time)
            Pattern pattern2 = Pattern.compile(
                    "([A-Za-z]{3})\\s+(\\d{1,2}),?\\s+(\\d{4})",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher2 = pattern2.matcher(text);

            if (matcher2.find()) {
                try {
                    String month = matcher2.group(1);
                    String day = matcher2.group(2);
                    String year = matcher2.group(3);

                    String monthNum = getMonthNumber(month);
                    String dayFormatted = String.format("%02d", Integer.parseInt(day));
                    String dateStr = year + "-" + monthNum + "-" + dayFormatted + "T00:00:00";

                    System.out.println("✅ Found date string (no time): " + month + " " + day + ", " + year);

                    LocalDateTime transactionDate = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    System.out.println("✅ Parsed Transaction Date: " + transactionDate);
                    return transactionDate;
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to parse pattern 2: " + e.getMessage());
                }
            }

            // Pattern 3: DD/MM/YYYY or DD-MM-YYYY
            Pattern pattern3 = Pattern.compile("(\\d{2})[/-](\\d{2})[/-](\\d{4})");
            Matcher matcher3 = pattern3.matcher(text);
            if (matcher3.find()) {
                try {
                    String day = matcher3.group(1);
                    String month = matcher3.group(2);
                    String year = matcher3.group(3);

                    String dateStr = year + "-" + month + "-" + day + "T00:00:00";
                    LocalDateTime date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    System.out.println("✅ Found date (DD/MM/YYYY): " + date);
                    return date;
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to parse DD/MM/YYYY: " + e.getMessage());
                }
            }

            // Pattern 4: YYYY-MM-DD
            Pattern pattern4 = Pattern.compile("(\\d{4})[/-](\\d{2})[/-](\\d{2})");
            Matcher matcher4 = pattern4.matcher(text);
            if (matcher4.find()) {
                try {
                    String dateStr = matcher4.group(0).replace("/", "-") + "T00:00:00";
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    System.out.println("✅ Found date (YYYY-MM-DD): " + dateTime);
                    return dateTime;
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to parse YYYY-MM-DD: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("⚠️ Date parsing error: " + e.getMessage());
        }

        System.out.println("⚠️ No Transaction Date found");
        return null;
    }

    /**
     * Helper method to convert month name to number
     */
    private String getMonthNumber(String monthName) {
        if (monthName == null) {
            return "01";
        }

        switch (monthName.toLowerCase()) {
            case "jan":
            case "january":
                return "01";
            case "feb":
            case "february":
                return "02";
            case "mar":
            case "march":
                return "03";
            case "apr":
            case "april":
                return "04";
            case "may":
                return "05";
            case "jun":
            case "june":
                return "06";
            case "jul":
            case "july":
                return "07";
            case "aug":
            case "august":
                return "08";
            case "sep":
            case "september":
                return "09";
            case "oct":
            case "october":
                return "10";
            case "nov":
            case "november":
                return "11";
            case "dec":
            case "december":
                return "12";
            default:
                System.out.println("⚠️ Unknown month: " + monthName + ", defaulting to 01");
                return "01";
        }
    }
}