package io.nextpos.einvoice.common.invoice;

import io.nextpos.einvoice.common.shared.EInvoiceBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Electronic Invoice specification:
 * https://www.einvoice.nat.gov.tw/home/DownLoad?fileName=1532427864696_0.pdf
 * <p>
 * QR Codes:
 * https://invoice.ppmof.gov.tw/web_doc/onlineBook/docs_A4.pdf
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ElectronicInvoice extends EInvoiceBaseObject {

    @Id
    private String id;

    private String clientId;

    private String orderId;

    /**
     * The official electronic invoice number
     */
    @Indexed(unique = true)
    private String invoiceNumber;

    /**
     * The invoice number without the hyphen separator (i.e. -)
     */
    private String internalInvoiceNumber;

    private InvoiceStatus invoiceStatus;

    /**
     * official invoice number period
     */
    private InvoicePeriod invoicePeriod;

    private String randomNumber;

    private Date invoiceCreatedDate;

    private BigDecimal salesAmount;

    private BigDecimal taxAmount;

    private String sellerUbn;

    private String sellerName;

    private String sellerAddress;

    private String barcodeContent;

    private String qrCode1Content;

    private String qrCode2Content;

    @Transient
    private String qrCode1ImageBinary;

    @Transient
    private String qrCode2ImageBinary;

    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    /**
     * The following fields are optional
     */
    private String buyerUbn;

    /**
     * Exists mainly for cancel order receipt XML.
     */
    private String buyerName;

    private CarrierType carrierType;

    private String carrierId;

    private String carrierId2;

    private String npoBan;

    private boolean printMark;

    public ElectronicInvoice(String clientId, String orderId, String invoiceNumber, InvoiceStatus invoiceStatus, InvoicePeriod invoicePeriod, BigDecimal salesAmount, BigDecimal taxAmount, String sellerUbn, String sellerName, String sellerAddress, List<InvoiceItem> invoiceItems) {
        this.clientId = clientId;
        this.orderId = orderId;
        this.updateInvoiceNumber(invoiceNumber);
        this.invoiceStatus = invoiceStatus;
        this.invoicePeriod = invoicePeriod;
        this.randomNumber = RandomStringUtils.randomNumeric(4);
        this.invoiceCreatedDate = new Date();

        this.updateSalesAndTaxAmount(salesAmount, taxAmount);

        this.sellerUbn = sellerUbn;
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.invoiceItems = invoiceItems;
    }

    public void updateInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
        this.internalInvoiceNumber = invoiceNumber.replace("-", "");
    }

    public void updateSalesAndTaxAmount(BigDecimal salesAmount, BigDecimal taxAmount) {
        this.salesAmount = salesAmount.setScale(0, RoundingMode.HALF_UP);
        this.taxAmount = taxAmount.setScale(0, RoundingMode.HALF_UP);
    }

    public boolean canPrintElectronicInvoice() {
        boolean buyerUbnWithPrintMark = carrierType != null && StringUtils.isNotBlank(buyerUbn) && printMark;
        boolean notCarrierAndDonation = carrierType == null && StringUtils.isBlank(npoBan);

        return buyerUbnWithPrintMark || notCarrierAndDonation;
    }

    public BigDecimal getSalesAmountWithoutTax() {
        return salesAmount.subtract(taxAmount);
    }

    public String getFormattedInvoiceDate() {
        return String.format("%s年%s-%s月", invoicePeriod.getYear(), invoicePeriod.getStartMonth(), invoicePeriod.getEndMonth());
    }

    public void generateCodeContent(String aesKey) {
        final InvoiceQRCodeEncryptor invoiceQRCodeEncryptor = new InvoiceQRCodeEncryptor(aesKey);

        this.generateBarcodeContent();
        this.generateQrCode1Content(invoiceQRCodeEncryptor);
        this.generateQrCode2Content();
    }

    private void generateBarcodeContent() {
        barcodeContent = invoicePeriod.formatInvoicePeriod() + internalInvoiceNumber + randomNumber;
    }

    private void generateQrCode1Content(InvoiceQRCodeEncryptor invoiceQRCodeEncryptor) {

        final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyMMdd")
                .withChronology(MinguoChronology.INSTANCE)
                .withZone(ZoneId.of("Asia/Taipei"));

        final StringBuilder qrCodeContent = new StringBuilder();
        qrCodeContent.append(internalInvoiceNumber);
        qrCodeContent.append(df.format(invoiceCreatedDate.toInstant()));
        qrCodeContent.append(randomNumber);
        qrCodeContent.append(toEightDigitHexadecimal(getSalesAmountWithoutTax()));
        qrCodeContent.append(toEightDigitHexadecimal(salesAmount));
        qrCodeContent.append(buyerUbn != null ? buyerUbn : "00000000");
        qrCodeContent.append(sellerUbn);
        qrCodeContent.append(encryptInvoiceNumber(invoiceQRCodeEncryptor)).append(":");
        qrCodeContent.append("**********").append(":");

        qrCodeContent.append(invoiceItems.size()).append(":");
        qrCodeContent.append(invoiceItems.size()).append(":");
        qrCodeContent.append("1: "); // 0 Big-5, 1 UTF-8, 2 Base64

//        if (qrCodeContent.length() < 135) {
//            qrCodeContent.append(" ".repeat(135 - qrCodeContent.length()));
//        }

        this.qrCode1Content = qrCodeContent.toString();
    }

    private String toEightDigitHexadecimal(BigDecimal number) {

        final String hex = Long.toHexString(number.longValue());
        final StringBuilder leftPadding = new StringBuilder();

        for (int i = 0; i < 8 - hex.length(); i++) {
            leftPadding.append("0");
        }

        return leftPadding.append(hex).toString();
    }

    private String encryptInvoiceNumber(final InvoiceQRCodeEncryptor invoiceQRCodeEncryptor) {
        return invoiceQRCodeEncryptor.encode(internalInvoiceNumber + randomNumber);
    }

    private void generateQrCode2Content() {

        final StringBuilder qrCodeContent = new StringBuilder();

        for (final InvoiceItem lineItem : invoiceItems) {
            qrCodeContent.append(lineItem.getProductName()).append(":")
                    .append(lineItem.getQuantity()).append(":")
                    .append(lineItem.getUnitPrice()).append(":");
        }

        //this.qrCode2Content = "**" + Base64.getEncoder().encodeToString(qrCodeContent.toString().getBytes(StandardCharsets.UTF_8));
        //this.qrCode2Content = "**" + qrCodeContent.toString();
        this.qrCode2Content = "**";

//        if (this.qrCode2Content.length() < 135) {
//            this.qrCode2Content += " ".repeat(135 - this.qrCode2Content.length());
//        }
    }

    public String getQrCode1ContentAsHex() {
        return encodeContentAsHex(qrCode1Content);
    }

    public String getQrCode2ContentAsHex() {
        return encodeContentAsHex(qrCode2Content);
    }

    private String encodeContentAsHex(String content) {

        final StringBuilder command = new StringBuilder("1D286B");
        final String byteLength1 = String.format("%02X", (content.length() + 3) % 256);
        final String byteLength2 = String.format("%02X", (content.length() + 3) / 256);
        command.append(byteLength1).append(byteLength2);
        command.append("315030");
        command.append(Hex.encodeHex(content.getBytes(StandardCharsets.UTF_8), false));

        return command.toString();
    }

    public enum CarrierType {
        MOBILE, CITIZEN_CERTIFICATE
    }

    @Data
    @NoArgsConstructor
    public static class InvoicePeriod {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM");

        /**
         * Minguo year (e.g. 109 = 2020)
         */
        private String year;

        /**
         * Always start in odd month.
         *
         * e.g. 09
         */
        private String startMonth;

        /**
         * Always end in even month.
         *
         * e.g. 10
         */
        private String endMonth;

        public InvoicePeriod(ZoneId zoneId) {

            final ChronoZonedDateTime<MinguoDate> date = MinguoChronology.INSTANCE.zonedDateTime(Instant.now(), zoneId);
            this.year = String.valueOf(date.get(ChronoField.YEAR));
            final int month = date.get(ChronoField.MONTH_OF_YEAR);

            if (month % 2 == 0) {
                this.startMonth = date.minus(1, ChronoUnit.MONTHS).format(FORMATTER);
                this.endMonth = date.format(FORMATTER);
            } else {
                this.startMonth = date.format(FORMATTER);
                this.endMonth = date.plus(1, ChronoUnit.MONTHS).format(FORMATTER);
            }
        }

        public String formatInvoicePeriod() {
            return getYear() + getEndMonth();
        }

        public String formatLongInvoicePeriod() {
            return getYear() + getStartMonth() + getEndMonth();
        }
    }

    @Data
    @AllArgsConstructor
    public static class InvoiceItem {

        private String productName;

        private int quantity;

        private BigDecimal unitPrice;

        private BigDecimal subTotal;
    }

    public enum InvoiceStatus {

        /**
         * Initial state.
         */
        CREATED,

        INVOICE_NUMBER_MISSING,

        /**
         * MIG file created and copied to upload directory.
         */
        MIG_CREATED,

        /**
         * Electronic invoice is processed successfully.
         */
        PROCESSED,

        /**
         * Electronic invoice is cancelled.
         */
        CANCELLED,

        /**
         * Electronic invoice is void.
         */
        VOID
    }

    /**
     * @author MrCuteJacky
     * @version 1.0
     */
    public static class InvoiceQRCodeEncryptor {

        /**
         * The SPEC type
         */
        private final static String TYPE_SPEC = "AES";

        /**
         * The INIT type.
         */
        private final static String TYPE_INIT = "AES/CBC/PKCS5Padding";

        /**
         * The SPEC key.
         */
        private final static String SPEC_KEY = "Dt8lyToo17X/XkXaQvihuA==";

        private final SecretKeySpec secretKeySpec;

        private final Cipher cipher;

        private final IvParameterSpec ivParameterSpec;

        public InvoiceQRCodeEncryptor(String aesKey) {

            try {
                ivParameterSpec = new IvParameterSpec(DatatypeConverter.parseBase64Binary(SPEC_KEY));
                secretKeySpec = new SecretKeySpec(DatatypeConverter.parseHexBinary(aesKey), TYPE_SPEC);
                cipher = Cipher.getInstance(TYPE_INIT);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create QR Code Encryptor: " + e.getMessage());
            }

        }

        public String encode(String input) {

            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
                byte[] encoded = cipher.doFinal(input.getBytes());

                return DatatypeConverter.printBase64Binary(encoded);
            } catch (Exception e) {
                throw new RuntimeException("Unable to encrypt string: " + e.getMessage());
            }
        }

        public String decode(String input) throws Exception {

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decoded = DatatypeConverter.parseBase64Binary(input);

            return new String(cipher.doFinal(decoded));
        }
    }
}
