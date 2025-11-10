package com.example.restrosuite.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
public class QrCodeService {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    /**
     * Generate QR code for a table and return as base64 string
     */
    public String generateQrCodeBase64(UUID tableId) throws WriterException, IOException {
        String qrContent = generateQrCodeUrl(tableId);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * Generate QR code URL for a table
     * This URL should point to the frontend, not the backend
     */
    public String generateQrCodeUrl(UUID tableId) {
        String url = frontendUrl;
        if (url == null || url.isEmpty()) {
            url = "http://localhost:5173";
        }
        // Ensure URL doesn't end with a slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        // Ensure URL has protocol
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url + "/customer/order?tableId=" + tableId.toString();
    }
}

