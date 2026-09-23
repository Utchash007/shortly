package com.shortly.app.controller;

import com.shortly.app.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QR code endpoints serving PNG images of short URLs.
 */
@RestController
@RequestMapping("/api/urls")
@Tag(name = "QR Codes", description = "QR code images")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    /**
     * Creates the controller with its service collaborator.
     *
     * @param qrCodeService QR rendering
     */
    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * Returns the QR code for one link as a PNG image.
     *
     * @param id the database identifier of the link
     * @return 200 with {@code image/png} bytes
     */
    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get the QR code for a short URL")
    @ApiResponse(responseCode = "200", description = "PNG image returned")
    @ApiResponse(responseCode = "404", description = "Unknown id")
    public ResponseEntity<byte[]> getQr(@PathVariable Long id) {
        byte[] png = qrCodeService.qrForUrl(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
