package com.dinsho.solo.Service;

import com.dinsho.solo.Utils;
import com.dinsho.solo.Model.CommonConstants;
import com.dinsho.solo.Model.Constants;
import com.dinsho.solo.Model.Loan;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
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
import java.util.*;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class LoanFunder {
    private Constants constants;

    private final static String LOAN_REQUEST_PATH = "data/requests/requestList.json";

    private final static String LOAN_GRID_LOCATOR = ".MuiGrid-root > div";
    private final static String BORROWER_LOCATER_LIST_PAGE = "//*[@id='__next']/main/div[3]/div[2]/div[%s]/div/div[1]/h5";
    private final static String LOAN_REASON_LOCATOR_LIST_PAGE = "//*[@id='__next']/main/div[3]/div[2]/div[%s]/div/div[2]/div[4]/h5[2]";

    private final static String CURR_LOAN_LOCATOR = "//*[@id='__next']/main/div[3]/div[2]/div[%s]/div";

    private final static String SLP_CHECKBOX_LOCATOR = "input[type=\"checkbox\"]";

    private final static String PROCEED_TO_FUND = "Proceed to Fund";
    private final static String AGREED_TO_LEND = "input[type=\"checkbox\"]";
    private final static String LEND_NOW = "Lend Now";

    private Map<String, Long> LoanRequested;
    private Gson gsonReader;

    public LoanFunder() {
        LoanRequested = new HashMap<>();
        gsonReader = new GsonBuilder().create();
    }

    public void ExecuteLoan() throws InterruptedException, JsonSyntaxException, JsonIOException, FileNotFoundException {

        constants = Utils.getConstants("requested");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().connectOverCDP(CommonConstants.CHROME_DEV_PROTOCOL);
            BrowserContext defaultContext = browser.contexts().get(0);
            Page page = defaultContext.pages().get(0);

            page.navigate(CommonConstants.SOLO_ENDPOINT);

            goBackToMarket(page);
            Locator LoanList = page.locator(LOAN_GRID_LOCATOR);

            int loanCount = LoanList.count();

            LoanModule loanModule = new LoanModule(constants);

            for (int i = 1; i <= loanCount; i++) {

                // String borrowerLocator = String.format(BORROWER_LOCATER_LIST_PAGE,
                // String.valueOf(i));
                String strValue = String.valueOf(i);
                String borrower = page.locator(String.format(BORROWER_LOCATER_LIST_PAGE, strValue)).last().textContent()
                        .trim();

                // String loanReasonLocator = String.format(LOAN_REASON_LOCATOR_LIST_PAGE,
                // strValue);
                String loanReason = page.locator(
                        String.format(LOAN_REASON_LOCATOR_LIST_PAGE, strValue)).last().textContent().trim();

                String key = Utils.getMapKey(borrower, loanReason);

                Map<String, Long> requests = getRequestedLoans(LOAN_REQUEST_PATH);

                if (requests == null) {
                    requests = new HashMap<>();
                }

                if (requests.size() == 0 || !requests.containsKey(key)) {

                    // Update the key
                    requests.put(key, Instant.now().getEpochSecond());
                    updateRequestedLoans(requests, LOAN_REQUEST_PATH);

                    // String currLoanLocator = String.format(CURR_LOAN_LOCATOR, strValue);
                    page.locator(String.format(CURR_LOAN_LOCATOR, strValue)).click();

                    assertThat(page).hasURL(CommonConstants.SOLO_FUNDING_ENDPOINT);

                    // Thread.sleep(1000);

                    Loan loan = loanModule.getLoanDetailV2(page, i);

                    if (loan != null && (loan.IsLendable() || loan.approveOrange() || loan.approvedBlue())) {

                        Locator SLPCheckBox = page.locator(SLP_CHECKBOX_LOCATOR);

                        // Edit tips
                        if (loan.getTipPercentage() < 13) {
                            double tip = loan.getLoanAmount() * .15;
                            page.getByText("Edit Tip").first().click();
                            page.getByLabel("Example: $10.00").fill(String.valueOf(tip));
                            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
                        }

                        // Aplly SLP
                        SLPCheckBox.check();

                        // Proceed to fund
                        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(PROCEED_TO_FUND)).click();

                        // Lend now
                        Thread.sleep(1000);
                        page.locator(AGREED_TO_LEND).check();
                        // page.getByRole(AriaRole.BUTTON, new
                        // Page.GetByRoleOptions().setName(LEND_NOW)).click();
                    }
                    goBackToMarket(page);
                }
            }
        } catch (

        Exception e) {
            System.out.println(e);
        }
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

    private void goBackToMarket(Page page) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CommonConstants.MARKET_BUTTON_LOCATOR))
                .click();
        assertThat(page).hasURL(CommonConstants.MARKET_URL);
    }
}
