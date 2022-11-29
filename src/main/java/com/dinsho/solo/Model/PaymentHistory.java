package com.dinsho.solo.Model;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.SQLInsert;

@Entity
@SQLInsert(sql = "INSERT IGNORE INTO payment_history(borrower,paid_amount,paid_date,payment_status,id)VALUES(?,?,?,?,?)")
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "borrower", "paidAmount", "paidDate" }) })
public class PaymentHistory {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String borrower;
    private double paidAmount;
    private ZonedDateTime paidDate;
    private String paymentStatus;

    public PaymentHistory() {

    }

    public PaymentHistory(String borrower, double paidAmount, ZonedDateTime paidDate, String paymentStatus) {
        this.borrower = borrower;
        this.paidAmount = paidAmount;
        this.paidDate = paidDate;
        this.paymentStatus = paymentStatus;
    }

    /**
     * @return double return the paidAmount
     */
    public double getPaidAmount() {
        return paidAmount;
    }

    /**
     * @param paidAmount the paidAmount to set
     */
    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    /**
     * @return ZonedDateTime return the paidDate
     */
    public ZonedDateTime getPaidDate() {
        return paidDate;
    }

    /**
     * @param paidDate the paidDate to set
     */
    public void setPaidDate(ZonedDateTime paidDate) {
        this.paidDate = paidDate;
    }

    /**
     * @return boolean return the onTime
     */
    public String getPaymentStatus() {
        return paymentStatus;
    }

    /**
     * @param onTime the onTime to set
     */
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    /**
     * @return int return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return String return the borrower
     */
    public String getBorrower() {
        return borrower;
    }

    /**
     * @param borrower the borrower to set
     */
    public void setBorrower(String borrower) {
        this.borrower = borrower;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (this.getClass() != other.getClass()) {
            return false;
        }
        PaymentHistory py = (PaymentHistory) other;
        return (this.borrower.equals(py.borrower) && this.paidDate.equals(py.paidDate)
                && Double.compare(this.paidAmount, py.paidAmount) == 0);
    }
}
