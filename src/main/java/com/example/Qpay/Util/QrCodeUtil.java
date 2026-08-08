package com.example.Qpay.Util;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class QrCodeUtil {

    @Value("${qr.size:300}")
    private int qrSize;

    /**
     * Generate a secure random QR token.
     */
    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a QR code PNG image from content, returned as base64 string.
     */
    public String generateQrBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code: {}", e.getMessage());
            throw new RuntimeException("QR code generation failed", e);
        }
    }

    /**
     * Generate a store QR code token (used once during store setup).
     */
    public String generateStoreQrToken(UUID storeId) {
        return "SC-STORE-" + storeId.toString().replace("-", "").toUpperCase();
    }
}
