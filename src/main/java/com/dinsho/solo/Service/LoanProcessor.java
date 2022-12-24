package com.dinsho.solo.Service;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.io.FileNotFoundException;
import java.security.cert.CertPathValidatorException.Reason;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dinsho.solo.Utils;
import com.dinsho.solo.Model.CommonConstants;
import com.dinsho.solo.Model.Constants;
import com.dinsho.solo.Model.Loan;
import com.dinsho.solo.Model.PaymentHistory;
import com.dinsho.solo.Repository.LoanRepository;
import com.dinsho.solo.Repository.PaymentHistoryRepository;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

@Service
public class LoanProcessor {

    @Autowired
    private LoanRepository loanRepository;
    @Autowired

    private PaymentHistoryRepository paymentHistoryRepository;

    private Constants constants;

    private final static String LOAN_GRID_LOCATOR = "//*[@id='__next']/main/div/div/div[3]/div/div[%s]/div[2]";

    public LoanProcessor() {
        // this.constants = constants;
    }

    public void ExecuteLoan() throws InterruptedException, JsonSyntaxException, JsonIOException, FileNotFoundException {
        constants = Utils.getConstants("loaned");
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().connectOverCDP(CommonConstants.CHROME_DEV_PROTOCOL);
            BrowserContext defaultContext = browser.contexts().get(0);
            Page page = defaultContext.pages().get(0);

            page.navigate(CommonConstants.SOLO_ENDPOINT);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Loans")).click();
            assertThat(page).hasURL("https://app.solofunds.io/loans?value=current");

            page.getByRole(AriaRole.COMBOBOX).click();

            Pattern pattern = Pattern.compile("Funded(.*)");

            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(pattern)).click();

            int loanCount = 500;
            LoanModule loanModule = new LoanModule(constants);
            String borrowerLocator = "//*[@id='__next']/main/div/div/div[3]/div/div[%s]/div[2]/div/div[1]/h5";
            String loanResaonLocator = "//*[@id='__next']/main/div/div/div[3]/div/div[%s]/div[2]/div/div[2]/div[4]/h5[2]";
            String paybackLocator = "//*[@id='__next']/main/div/div/div[3]/div/div[%s]/div[2]/div/div[2]/div[3]/h5[2]";

            //// *[@id="__next"]/main/div/div/div[3]/div/div[437]/div[1]
            //// *[@id="__next"]/main/div/div/div[3]/div/div[436]/div[1]/span
            //// *[@id="__next"]/main/div/div/div[3]/div/div[438]/div[1]/span
            //// *[@id="__next"]/main/div/div/div[3]/div/div[435]/div[1]/span
            for (int i = 1; i <= loanCount; i++) {
                String LoanStatus = page.locator(String
                        .format("//*[@id='__next']/main/div/div/div[3]/div/div[%s]/div[1]/span", String.valueOf(i)))
                        .textContent();

                // Only run for funded loan
                if (LoanStatus.equals("Funded")) {
                    String borrowerName = page.locator(String.format(borrowerLocator, String.valueOf(i))).textContent()
                            .trim();
                    String loanReason = page.locator(String.format(loanResaonLocator, String.valueOf(i))).textContent()
                            .trim();

                    String paybackDateText = page.locator(String.format(paybackLocator,
                            String.valueOf(i))).textContent();
                    ZonedDateTime paybackDate = ZonedDateTime.now()
                            .plusDays(Utils.getNumberFromString(paybackDateText));
                    if (!borrowerName.equals("Bryan C.")) {
                        if (loanRepository.findByQuery(borrowerName, loanReason, paybackDate).isEmpty()) {
                            String currLoanLocator = String.format(LOAN_GRID_LOCATOR, String.valueOf(i));
                            page.locator(currLoanLocator).click();
                            assertThat(page).hasURL("https://app.solofunds.io/loans/detail");

                            Loan loan = loanModule.getLoanDetailV2(page, i);
                            if (loan == null) {
                                System.out.println(
                                        "unable to get loan with borrowe :" + borrowerName + " reason : " + loanReason);
                            } else {
                                List<PaymentHistory> paymentHistory = paymentHistoryRepository
                                        .findByBorrower(loan.getBorrower());
                                removeDuplicatePaymentHistory(loan, paymentHistory);
                                loanRepository.save(loan);

                                page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("ArrowLeft")).click();
                                assertThat(page).hasURL("https://app.solofunds.io/loans");
                                page.locator("button:has-text(\"Current\")").click();
                            }
                        }
                    }
                }
            }
        } catch (

        Exception e) {
            // goBackToMarket(page);
            System.out.println(e);
        }
    }

    private Loan removeDuplicatePaymentHistory(Loan loan, List<PaymentHistory> pHistory) {

        PaymentHistory lastPayment = pHistory.isEmpty() ? null : pHistory.get(0);
        List<PaymentHistory> ph = new ArrayList<>();

        for (var payment : loan.getPaymentHistory()) {
            if (!ph.contains(payment)
                    && (lastPayment == null || payment.getPaidDate().compareTo(lastPayment.getPaidDate()) > 0)) {
                ph.add(payment);

            }
        }

        loan.setPaymentHistory(ph);

        return loan;
    }

    private void gotoLoan(Page page) {
        // page.getByRole(AriaRole.BUTTON, new
        // Page.GetByRoleOptions().setName(CommonConstants.MARKET_BUTTON_LOCATOR))
        // .click();
        // assertThat(page).hasURL(CommonConstants.MARKET_URL);
        //// *[@id="__next"]/header/div/div/div[4]/button[3]
    }

    /**
     * @return Constants return the constants
     */
    public Constants getConstants() {
        return constants;
    }

    /**
     * @param constants the constants to set
     */
    public void setConstants(Constants constants) {
        this.constants = constants;
    }

}