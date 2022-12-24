package com.dinsho.solo.Service;

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

public class Lender {
  private final static String SOLO_ENDPOINT = "https://app.solofunds.io/market";
  private final static String SOLO_FUNDING_ENDPOINT = "https://app.solofunds.io/funding";
  private final static String CHROME_DEV_PROTOCOL = "http://localhost:9222";

  private final static String PAYBACK_HISTORY_LOCATOR = "gear Payback History";
  private final static String SEE_MORE_LOCATOR = "Shows More";
  private final static String SLP_CHECKBOX_LOCATOR = "input[type=\"checkbox\"]";

  private final static String LOAN_GRID_LOCATOR = ".MuiGrid-root > div";

  private final static String BORROWER_LOCATER = "//*[@id='__next']/main/div/div/div/div[2]/div/div/div[1]/div[2]/div[1]/span[1]";
  private final static String BORROWER_LOCATER_LIST_PAGE = "//*[@id='__next']/main/div[3]/div[2]/div[%s]/div/div[1]/h5";
  private final static String NUM_OF_PAYMENTS_LOCATOR = "//*[@id='__next']/main/div/div/div/div[2]/div/div/div[1]/div[2]/div[1]/span[2]";
  private final static String SOLO_SCORE_LOCATOR = "//*[@id='__next']/main/div/div/div/div[2]/div/div/div[1]/div[1]/div/div[2]/div/div/div";

  private final static String LOAN_AMOUNT_LOCATOR = "//*[@id='__next']/main/div/div/div/div[3]/div/div[1]/div/div[2]/div[2]/span";
  private final static String LENDERS_TIP_LOCATOR_CASE1 = "//*[@id='__next']/main/div/div/div/div[3]/div/div[1]/div/div[4]/div[2]/span";
  private final static String LENDERS_TIP_LOCATOR_CASE2 = "//*[@id='__next']/main/div/div/div/div[3]/div/div[1]/div/div[3]/div[2]/span";
  private final static String PAYBACK_DATE_LOCATOR = "//*[@id='__next']/main/div/div/div/div[3]/div/div[1]/div/div[1]/span";
  private final static String LOAN_REASON_LOCATOR = "//*[@id='__next']/main/div/div/div/div[2]/div/div/div[1]/div[2]/div[2]/span[2]";
  private final static String LOAN_REASON_LOCATOR_LIST_PAGE = "//*[@id='__next']/main/div[3]/div[2]/div[%s]/div/div[2]/div[4]/h5[2]";

  private final static String PAYMENT_STATUS_LOCATOR = "//*[@id='__next']/main/div/div/div/div[2]/div/div/div[3]/div[%s]/div/p";
  private final static String PAYMENT_AMOUNT_LOCATOR = "//*[@id='__next']/main/div/div/div/div[2]/div/div/div[3]/div[%s]/div/p/span[1]";
  private final static String PAYMENT_DATE_LOCATOR = "//*[@id='__next']/main/div/div/div/div[2]/div/div/div[3]/div[%s]/div/p/span[2]";

  private final static String CURR_LOAN_LOCATOR = "//*[@id='__next']/main/div[3]/div[2]/div[%s]/div";

  private final static String PROCEED_TO_FUND = "Proceed to Fund";
  private final static String AGREED_TO_LEND = "input[type=\"checkbox\"]";
  private final static String LEND_NOW = "Lend Now";

  private final static String MARKET_BUTTON_LOCATOR = "Market";
  private final static String MARKET_URL = "https://app.solofunds.io/market";

  private final static String LOAN_REQUEST_PATH = "data/requests/requestList.json";

  private Map<String, Long> LoanRequested;
  private Gson gsonReader;

  public Lender() {
    LoanRequested = new HashMap<>();
    gsonReader = new GsonBuilder().create();
  }

  public void ExecuteLoan() throws InterruptedException {
    try (Playwright playwright = Playwright.create()) {
      Browser browser = playwright.chromium().connectOverCDP(CHROME_DEV_PROTOCOL);
      BrowserContext defaultContext = browser.contexts().get(0);
      Page page = defaultContext.pages().get(0);

      /*
       * page.onceDialog(dialog -> {
       * // if ("beforeunload".equals(dialog.type()))
       * dialog.accept();
       * });
       */
      page.navigate(SOLO_ENDPOINT);
      goBackToMarket(page);
      Locator LoanList = page.locator(LOAN_GRID_LOCATOR);

      int loanCount = LoanList.count();
      // System.out.println("count:" + loanCount);
      for (int i = 1; i < loanCount && i < 21; i++) {

        // goBackToMarket(page);

        String borrowerLocator = String.format(BORROWER_LOCATER_LIST_PAGE, String.valueOf(i));
        String borrower = page.locator(borrowerLocator).last().textContent().trim();

        String loanReasonLocator = String.format(LOAN_REASON_LOCATOR_LIST_PAGE, String.valueOf(i));
        String loanReason = page.locator(loanReasonLocator).last().textContent().trim();

        String key = getMapKey(borrower, loanReason);

        Map<String, Long> requests = getRequestedLoans(LOAN_REQUEST_PATH);

        if (requests == null) {
          requests = new HashMap<>();
        }
        if (requests.size() == 0 || !requests.containsKey(key)) {

          // Update the key
          requests.put(key, Instant.now().getEpochSecond());
          updateRequestedLoans(requests, LOAN_REQUEST_PATH);

          String currLoanLocator = String.format(CURR_LOAN_LOCATOR, String.valueOf(i));
          page.locator(currLoanLocator).click();

          assertThat(page).hasURL(SOLO_FUNDING_ENDPOINT);
          page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(PAYBACK_HISTORY_LOCATOR)).click();
          // Thread.sleep(1000);

          Loan loan = getLoanDetailV2(page);

          if (loan != null && (loan.IsLendable() || loan.approveOrange() || loan.approvedBlue())) {

            Locator SLPCheckBox = page.locator(SLP_CHECKBOX_LOCATOR);

            // Edit tips
            if (loan.getTipPercentage() < 13) {
              double tip = loan.getLoanAmount() * .15;
              page.getByText("Edit Tip").first().click();
              page.getByLabel("Example: $10.00").fill(String.valueOf(tip));
              page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
            }

            SLPCheckBox.check();
            // Proceed to fund
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(PROCEED_TO_FUND)).click();

            // writeToFile(loan);
            // String json = gsonReader.toJson(loan);
            // String filePath = "data/borrowers/" + loan.getBorrower().replace(" ", "") +
            // "_loans.json";
            // PrintWriter pw = new PrintWriter(filePath);
            // pw.write(json);

            // pw.flush();
            // pw.close();

            // Lend now
            Thread.sleep(1000);
            page.locator(AGREED_TO_LEND).check();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(LEND_NOW)).click();
          }

          goBackToMarket(page);
        }
      }
    } catch (

    Exception e) {
      // goBackToMarket(page);
      System.out.println(e);
    }
  }

  public Loan getLoanDetailV2(Page page) {
    try {
      String borrower = page.locator(BORROWER_LOCATER).first().textContent();

      String numofRepaidLoans = page.locator(NUM_OF_PAYMENTS_LOCATOR).last().textContent().replaceAll("[^0-9]", "");
      int numsOfLoanRepaid = NullOrEmpty(numofRepaidLoans) ? 0 : Integer.parseInt(numofRepaidLoans);

      String soloScoreText = page.locator(SOLO_SCORE_LOCATOR).last().textContent().trim();
      int soloScore = NullOrEmpty(soloScoreText) ? 0 : Integer.parseInt(soloScoreText);

      String loanAnmountText = page.locator(LOAN_AMOUNT_LOCATOR).last().textContent();
      Double loanAmount = NullOrEmpty(loanAnmountText) ? 0 : getAmount(loanAnmountText);

      String lendersTipText = "";
      if (page.getByText("SoLo Donation").count() > 0) {
        lendersTipText = page.locator(LENDERS_TIP_LOCATOR_CASE1).first().textContent();
      } else {
        lendersTipText = page.locator(LENDERS_TIP_LOCATOR_CASE2).first().textContent();
      }
      double lendersTip = NullOrEmpty(lendersTipText) ? 0 : getAmount(lendersTipText.trim());

      String paybackDateText = page.locator(PAYBACK_DATE_LOCATOR).first().textContent();

      ZonedDateTime paybackDate = getPaybackdate(paybackDateText);

      String loanReason = page.locator(LOAN_REASON_LOCATOR).last().textContent().trim();

      Instant now = Instant.now();
      ZonedDateTime currDate = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());

      Locator seeMore = page.getByText(SEE_MORE_LOCATOR);

      if (numsOfLoanRepaid > 3) {
        seeMore.click();
      }
      List<PaymentHistory> pHistory = getPaymentHistory(page, numsOfLoanRepaid, borrower);

      return new Loan();/*
                         * (currDate, soloScore, loanAmount, lendersTip, borrower,
                         * paybackDate, numsOfLoanRepaid, loanReason,
                         * pHistory, "Requested");
                         */
    } catch (

    Exception ex) {
      System.out.println(ex);
    }
    return null;
  }

  public List<PaymentHistory> getPaymentHistory(Page page, int paymentCount, String borrower) {

    try {

      List<PaymentHistory> result = new ArrayList<>();
      for (int i = 1; i < paymentCount; i++) {

        String paymentStatusLocator = String.format(PAYMENT_STATUS_LOCATOR, String.valueOf(i));
        String paymentText = page.locator(paymentStatusLocator).first().textContent().trim();
        String paymentStatus = paymentText.toLowerCase().indexOf("late") > 0 ? "late" : "ontime";

        String paidAmountLocator = String.format(PAYMENT_AMOUNT_LOCATOR, String.valueOf(i));
        String paidAmount = page.locator(paidAmountLocator).first().textContent().trim();
        Double paymentAmount = getAmount(paidAmount);

        String paybackDateLocator = String.format(PAYMENT_DATE_LOCATOR, String.valueOf(i));
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

  private void goBackToMarket(Page page) {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(MARKET_BUTTON_LOCATOR)).click();
    assertThat(page).hasURL(MARKET_URL);
  }

  private ZonedDateTime getPaybackdate(String paybackDate) {
    String cleanedPayback = paybackDate.replace("Receiving", "").replaceAll(":", "").trim();
    return getDateFromString(cleanedPayback);
  }

  private String getMapKey(String borrower, String loanReason) {

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

  private Map<String, Long> getRequestedLoans(String filePath) {
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

  private void updateRequestedLoans(Map<String, Long> loans, String filePath) {

    // Remove loan requests more than 48 hours old
    Long curr = Instant.now().getEpochSecond();
    loans.entrySet().removeIf(loan -> (loan.getValue() <= curr - 48 * 60 * 60));

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
