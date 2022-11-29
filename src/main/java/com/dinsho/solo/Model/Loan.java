package com.dinsho.solo.Model;

import java.beans.Transient;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "borrower", "loanReason", "paybackDate" }) })
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", unique = true, nullable = false)
    private long loanId;
    private ZonedDateTime fundingDate;
    private int soloScore;
    private double loanAmount;
    private double lendersTip;
    private String borrower;
    private ZonedDateTime paybackDate;
    private int numsOfLoanRepaid;
    private int numsOfLatePayments;
    private String loanReason;
    private String status;

    @ManyToMany(fetch = FetchType.LAZY, targetEntity = PaymentHistory.class, cascade = CascadeType.ALL)
    List<PaymentHistory> paymentHistory;

    public Loan() {

    }

    public Loan(ZonedDateTime fundingDate, int soloScore, double loanAmount, double lendersTip, String borrower,
            ZonedDateTime paybackDate, int numsOfLoanRepaid, String loanReason,
            List<PaymentHistory> payment,
            String status) {
        this.fundingDate = fundingDate;
        this.soloScore = soloScore;
        this.loanAmount = loanAmount;
        this.lendersTip = lendersTip;
        this.borrower = borrower;
        this.paybackDate = paybackDate;
        this.numsOfLoanRepaid = numsOfLoanRepaid;
        this.loanReason = loanReason;
        this.paymentHistory = payment;
        this.numsOfLatePayments = this.getNumOfLatePayments();
        this.status = status;
        // this.loanId = borrower + "_" + loanReason + "-" + paybackDate.toLocalDate();
    }

    public long getloanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public ZonedDateTime getFundingDate() {
        return fundingDate;
    }

    public void setFundingDate(ZonedDateTime fundingDate) {
        this.fundingDate = fundingDate;
    }

    public int getSoloScore() {
        return soloScore;
    }

    public void setSoloScore(int soloScore) {
        this.soloScore = soloScore;
    }

    public double getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(double loanAmount) {
        this.loanAmount = loanAmount;
    }

    public double getLendersTip() {
        return lendersTip;
    }

    public void setLendersTip(double lendersTip) {
        this.lendersTip = lendersTip;
    }

    public String getBorrower() {
        return borrower;
    }

    public void setBorrower(String borrower) {
        this.borrower = borrower;
    }

    public ZonedDateTime getPaybackDate() {
        return paybackDate;
    }

    public void setPaybackDate(ZonedDateTime paybackDate) {
        this.paybackDate = paybackDate;
    }

    public int getNumsOfLoanRepaid() {
        return numsOfLoanRepaid;
    }

    public void setNumsOfLoanRepaid(int numsOfLoanRepaid) {
        this.numsOfLoanRepaid = numsOfLoanRepaid;
    }

    public String getLoanReason() {
        return loanReason;
    }

    public void setLoanReason(String loanReason) {
        this.loanReason = loanReason;
    }

    public int getNumsOfLatePayments() {
        return numsOfLatePayments;
    }

    public void setNumsOfLatePayments(int numsOfLatePayments) {
        this.numsOfLatePayments = numsOfLatePayments;
    }

    public boolean isLendable(int soloScore, double lendersTip) {
        return this.soloScore >= soloScore && this.lendersTip >= lendersTip;
    }

    // @Transient
    public List<PaymentHistory> getPaymentHistory() {
        return paymentHistory;
    }

    public void setPaymentHistory(List<PaymentHistory> paymentHistory) {
        this.paymentHistory = paymentHistory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean IsPotentiallyLendable() {
        return (this.getSoloScore() >= 85);
    }

    public boolean IsLendable() {
        return this.IsPotentiallyLendable() && this.getOnTimeRate() >= 90;
    }

    private double getOnTimeRate() {
        if (this.numsOfLoanRepaid == 0)
            return 0;
        return ((double) (this.numsOfLoanRepaid - this.numsOfLatePayments) / this.numsOfLoanRepaid) * 100;
    }

    public double getTipPercentage() {
        return (this.getLendersTip() / this.getLoanAmount()) * 100;
    }

    private int getNumOfLatePayments() {

        return this.numsOfLatePayments = (int) paymentHistory.stream()
                .filter(payment -> payment.getPaymentStatus().equals("late")).count();
    }

    public boolean equals(Loan other) {
        if (other == null) {
            return false;
        }

        if (this.getClass() != other.getClass()) {
            return false;
        }
        return (this.borrower.equals(other.borrower) && this.loanReason.equals(other.loanReason)
                && this.paybackDate.equals(other.paybackDate));
    }

    private boolean hasLatePayment() {

        return paymentHistory.stream()
                .filter(payment -> payment.getPaymentStatus().equals("late")).count() > 0;
    }

    public boolean approved65To75() {
        ZonedDateTime curr = ZonedDateTime.now();
        return (!this.hasLatePayment()
                && (this.getPaybackDate().minusDays(8).toEpochSecond() < curr.toEpochSecond())
                && this.getSoloScore() >= 65);
    }

    public boolean approved75To85() {
        ZonedDateTime curr = ZonedDateTime.now();
        return (!this.hasLatePayment()
                && (this.getPaybackDate().minusDays(11).toEpochSecond() < curr.toEpochSecond())
                && this.getSoloScore() >= 75);
    }
}
