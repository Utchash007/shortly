package com.shortly.app.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * On-demand QR code generation for shortened URLs.
 *
 * <p>Images are rendered per request with ZXing and never stored: a QR code is
 * a deterministic function of the short URL. Error correction level
 * {@code M} tolerates minor damage while keeping the matrix dense.
 */
@Service
public class QrCodeService {

    /**
     * Default QR image edge in pixels.
     */
    public static final int DEFAULT_SIZE = 256;

    private final UrlService urlService;
    private final String baseUrl;

    /**
     * Creates the service with its required collaborators.
     *
     * @param urlService lookup of short codes by database id
     * @param baseUrl public application host encoded into the QR, from {@code app.base-url}
     */
    public QrCodeService(UrlService urlService,
                         @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.urlService = urlService;
        this.baseUrl = baseUrl;
    }

    /**
     * Renders the QR code for one link.
     *
     * @param id the database identifier of the link
     * @return PNG bytes encoding {@code {baseUrl}/{shortCode}}
     * @throws com.shortly.app.exception.UrlNotFoundException when the id is unknown
     */
    public byte[] qrForUrl(Long id) {
        String shortCode = urlService.getUrl(id).shortCode();
        return generatePng(baseUrl + "/" + shortCode, DEFAULT_SIZE);
    }

    /**
     * Renders arbitrary text as a PNG QR code.
     *
     * @param text the content to encode, must not be blank
     * @param size image edge in pixels, must be positive
     * @return PNG bytes
     * @throws IllegalArgumentException for blank text or non-positive size
     * @throws IllegalStateException when ZXing cannot encode the content
     */
    public byte[] generatePng(String text, int size) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    text, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not generate QR code", e);
        }
    }
}
