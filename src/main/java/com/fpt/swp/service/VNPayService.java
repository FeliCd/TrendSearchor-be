package com.fpt.swp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tích hợp cổng thanh toán VNPay (sandbox/production).
 *
 * <p>Chỉ lo phần mật mã + build URL theo chuẩn VNPay 2.1.0:
 * <ul>
 *   <li>{@link #createPaymentUrl} — tạo URL thanh toán đã ký HMAC-SHA512</li>
 *   <li>{@link #verifySignature} — verify chữ ký khi VNPay callback (return/IPN) trả về</li>
 * </ul>
 *
 * Credentials lấy từ env: {@code VNP_TMN_CODE}, {@code VNP_HASH_SECRET}. Chưa cấu hình
 * thì {@link #isConfigured()} = false và {@link #createPaymentUrl} ném lỗi rõ ràng.
 */
@Service
@Slf4j
public class VNPayService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final String version;
    private final String command;
    private final String currency;
    private final String locale;

    public VNPayService(
            @Value("${app.vnpay.tmn-code:}") String tmnCode,
            @Value("${app.vnpay.hash-secret:}") String hashSecret,
            @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
            @Value("${app.vnpay.return-url:http://localhost:8080/api/payments/vnpay/return}") String returnUrl,
            @Value("${app.vnpay.version:2.1.0}") String version,
            @Value("${app.vnpay.command:pay}") String command,
            @Value("${app.vnpay.currency:VND}") String currency,
            @Value("${app.vnpay.locale:vn}") String locale) {
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.payUrl = payUrl;
        this.returnUrl = returnUrl;
        this.version = version;
        this.command = command;
        this.currency = currency;
        this.locale = locale;
    }

    /** Đã cấu hình credentials chưa (có TmnCode + HashSecret). */
    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isBlank()
                && hashSecret != null && !hashSecret.isBlank();
    }

    /**
     * Tạo URL thanh toán VNPay cho một giao dịch.
     *
     * @param txnRef    mã tham chiếu duy nhất của giao dịch — VNPay echo lại khi callback
     * @param amount    số tiền (VND)
     * @param orderInfo mô tả đơn (nên là ASCII không dấu)
     * @param clientIp  IP của người dùng
     * @return URL đầy đủ để redirect trình duyệt sang VNPay
     */
    public String createPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String clientIp) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "VNPay chưa được cấu hình. Hãy set VNP_TMN_CODE và VNP_HASH_SECRET trong .env.");
        }

        LocalDateTime now = LocalDateTime.now(VN);
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValueExact(); // VNPay: số tiền × 100

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", version);
        params.put("vnp_Command", command);
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", currency);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo != null ? orderInfo : ("Thanh toan " + txnRef));
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", (locale != null && !locale.isBlank()) ? locale : "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1");
        params.put("vnp_CreateDate", now.format(FMT));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(FMT));

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String name = itr.next();
            String value = params.get(name);
            if (value == null || value.isEmpty()) continue;
            // hashData: raw name = url-encoded value ; query: url-encoded name = url-encoded value
            String encValue = URLEncoder.encode(value, StandardCharsets.US_ASCII);
            hashData.append(name).append('=').append(encValue);
            query.append(URLEncoder.encode(name, StandardCharsets.US_ASCII)).append('=').append(encValue);
            if (itr.hasNext()) {
                hashData.append('&');
                query.append('&');
            }
        }

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        return payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Verify chữ ký của params callback (return/IPN). Loại vnp_SecureHash(+Type),
     * sắp xếp field, tính lại HMAC-SHA512 và so khớp (không phân biệt hoa/thường).
     */
    public boolean verifySignature(Map<String, String> allParams) {
        if (!isConfigured() || allParams == null) return false;
        String received = allParams.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;

        Map<String, String> params = new HashMap<>(allParams);
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String name = itr.next();
            String value = params.get(name);
            if (value == null || value.isEmpty()) continue;
            hashData.append(name).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            if (itr.hasNext()) hashData.append('&');
        }

        // Bỏ '&' thừa cuối chuỗi nếu field cuối rỗng bị bỏ qua.
        String data = hashData.toString();
        while (data.endsWith("&")) data = data.substring(0, data.length() - 1);

        String computed = hmacSHA512(hashSecret, data);
        return computed.equalsIgnoreCase(received);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA512 failed", e);
        }
    }
}
