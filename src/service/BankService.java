package service;

import domain.Account;
import domain.Transaction;

import java.util.List;

public interface BankService {
    String openAccount(String name, String email,String accountType);
    List<Account> listAccounts();

    void deposit(String accountNumber, Double amount, String note ,String idempotencyKey);

    void withdraw(String accountNumber, Double amount, String withdrawal,String idempotencyKey);

    void transfer(String from, String to, Double amount, String transfer,String idempotencyKey);


    List<Transaction> getStatement(String account);

    List<Account> searchAccountByCustomerName(String q);
}
