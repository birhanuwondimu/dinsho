package com.dinsho.solo.Service;

import com.dinsho.solo.Model.CommonConstants;
import com.dinsho.solo.Model.Constants;
import com.dinsho.solo.Model.Loan;
import com.dinsho.solo.Model.PaymentHistory;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public class LoanModule {

    private Map<String, Long> LoanRequested;
    private Gson gsonReader;
    private Constants constants;

    public LoanModule(Constants constants) {
        LoanRequested = new HashMap<>();
        gsonReader = new GsonBuilder().create();
        this.constants = constants;
    }

    public Loan getLoanDetailV2(Page page, int i) {
        String currIndex = String.valueOf(i);
        try {
            String borrower = page.locator(String.format(constants.getBORROWER_LOCATER(),
                    currIndex)).first().textContent();

            String numofRepaidLoans = page.locator(String.format(constants.getNUM_OF_PAYMENTS_LOCATOR(),
                    currIndex)).last()
                    .textContent().replaceAll("[^0-9]", "");
            int numsOfLoanRepaid = NullOrEmpty(numofRepaidLoans) ? 0 : Integer.parseInt(numofRepaidLoans);

            String soloScoreText = page.locator(String.format(constants.getSOLO_SCORE_LOCATOR(),
                    currIndex)).last()
                    .textContent().trim();
            int soloScore = NullOrEmpty(soloScoreText) ? 0 : Integer.parseInt(soloScoreText);

            String loanAnmountText = page.locator(String.format(constants.getLOAN_AMOUNT_LOCATOR(),
                    currIndex)).last()
                    .textContent();
            Double loanAmount = NullOrEmpty(loanAnmountText) ? 0 : getAmount(loanAnmountText);

            String lendersTipText = "";
            if (page.getByText("SoLo Donation").count() > 0) {
                lendersTipText = page.locator(String.format(constants.getLENDERS_TIP_LOCATOR_CASE1(),
                        currIndex)).first()
                        .textContent();
            } else {
                lendersTipText = page.locator(String.format(constants.getLENDERS_TIP_LOCATOR_CASE2(),
                        currIndex)).first()
                        .textContent();
            }
            double lendersTip = NullOrEmpty(lendersTipText) ? 0 : getAmount(lendersTipText.trim());

            String paybackDateText = page.locator(String.format(constants.getPAYBACK_DATE_LOCATOR(),
                    currIndex)).first()
                    .textContent();

            ZonedDateTime paybackDate = getPaybackdate(paybackDateText);

            String loanReason = page.locator(String.format(constants.getLOAN_REASON_LOCATOR(),
                    currIndex)).last().textContent()
                    .trim();

            Instant now = Instant.now();
            ZonedDateTime currDate = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());

            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(constants.getPAYBACK_HISTORY_LOCATOR())).click();
            Thread.sleep(1000);

            Locator seeMore = page.getByText(constants.getSEE_MORE_LOCATOR());

            if (numsOfLoanRepaid > 3) {
                seeMore.click();
            }
            List<PaymentHistory> pHistory = getPaymentHistory(page, numsOfLoanRepaid, borrower);

            return new Loan(currDate, soloScore, loanAmount, lendersTip, borrower, paybackDate, numsOfLoanRepaid,
                    loanReason, pHistory, "Funded");

        } catch (

        Exception ex) {
            System.out.println(ex);
        }
        return null;
    }

    public List<PaymentHistory> getPaymentHistory(Page page, int paymentCount, String borrower) {

        try {

            List<PaymentHistory> result = new ArrayList<>();
            for (int i = 1; i <= paymentCount; i++) {

                String paymentStatusLocator = String.format(constants.getPAYMENT_STATUS_LOCATOR(), String.valueOf(i));
                String paymentText = page.locator(paymentStatusLocator).first().textContent().trim();
                String paymentStatus = paymentText.toLowerCase().indexOf("late") > 0 ? "late" : "ontime";

                String paidAmountLocator = String.format(constants.getPAYMENT_AMOUNT_LOCATOR(), String.valueOf(i));
                String paidAmount = page.locator(paidAmountLocator).first().textContent().trim();
                Double paymentAmount = getAmount(paidAmount);

                String paybackDateLocator = String.format(constants.getPAYMENT_DATE_LOCATOR(), String.valueOf(i));
                String paymentDateText = page.locator(paybackDateLocator).last().textContent();
                ZonedDateTime paymentDate = getDateFromString(paymentDateText);

                result.add(new PaymentHistory(borrower, paymentAmount, paymentDate, paymentStatus));
            }

            return result;
        } catch (Exception ex) {
            System.out.println(ex);
        }

        return null;
    }

    private boolean NullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    private double getAmount(String tipText) {
        if (NullOrEmpty(tipText))
            return 0;

        return Double.parseDouble(tipText.replaceAll("[^0-9.]", ""));
    }

    private ZonedDateTime getDateFromString(String date) {

        if (NullOrEmpty(date)) {
            date = "Jan 1, 2020"; // Set the default to Jan 1 of 2020;
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        LocalDate localDate = LocalDate.parse(date.trim(), dateFormatter);
        return localDate.atStartOfDay(ZoneId.systemDefault());
    }

    public void goBackToMarket(Page page) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CommonConstants.MARKET_BUTTON_LOCATOR))
                .click();
        assertThat(page).hasURL(CommonConstants.MARKET_URL);
    }

    public ZonedDateTime getPaybackdate(String paybackDate) {
        String cleanedPayback = paybackDate.replace("Receiving", "").replaceAll(":", "").trim();
        return getDateFromString(cleanedPayback);
    }

    public String getMapKey(String borrower, String loanReason) {

        if (NullOrEmpty(borrower) && NullOrEmpty(loanReason))
            return null;

        String replace = "[^a-zA-Z0-9\\s]";
        if (NullOrEmpty(loanReason))
            return borrower.replace(replace, "").replaceAll(" ", "");

        if (NullOrEmpty(borrower))
            return loanReason.replace(replace, "").replaceAll(" ", "");

        return borrower.replace(replace, "").replaceAll(" ", "") + "_"
                + loanReason.replaceAll(replace, "").replaceAll(" ", "");
    }

    public Map<String, Long> getRequestedLoans(String filePath) {
        if (LoanRequested != null && LoanRequested.size() > 0)
            return LoanRequested;

        File file = new File(filePath);
        Map<String, Long> loanRequests = new HashMap<>();

        if (file.exists()) {
            try {
                String data = new String(Files.readAllBytes(Paths.get(filePath)));
                Type type = new TypeToken<Map<String, Long>>() {
                }.getType();
                loanRequests = gsonReader.fromJson(data, type);
            } catch (IOException e) {
                System.out.println(e);
            }
        }

        return loanRequests;
    }

    public void updateRequestedLoans(Map<String, Long> loans, String filePath) {
        for (String kset : loans.keySet()) {
            long timeStamp = loans.get(kset);
            Long curr = Instant.now().toEpochMilli();
            /*
             * if (curr > timeStamp + 24 * 60 * 60 * 1000) {
             * loans.remove(kset);
             * }
             */
        }

        String json = gsonReader.toJson(loans);
        try (PrintWriter pw = new PrintWriter(filePath)) {
            pw.write(json);

            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
