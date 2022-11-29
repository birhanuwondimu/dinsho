package com.dinsho.solo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dinsho.solo.Model.Loan;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    public final static String FIND_BY_BORROWER_QUERY = "SELECT * FROM loan WHERE borrower = :borrower AND loan_reason = :loanReason";
    // List<Loan> findByBorrower(String name);

    @Query(value = FIND_BY_BORROWER_QUERY, nativeQuery = true)
    List<Loan> findByQuery(@Param("borrower") String borrower, @Param("loanReason") String loanReason);

}