package com.dinsho.solo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinsho.solo.Model.PaymentHistory;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    public final static String FIND_BY_BORROWER_QUERY = "SELECT * FROM payment_history WHERE borrower= :borrower ORDER BY paid_date DESC LIMIT 1;";

    @Query(value = FIND_BY_BORROWER_QUERY, nativeQuery = true)
    List<PaymentHistory> findByBorrower(@Param("borrower") String borrower);

}
