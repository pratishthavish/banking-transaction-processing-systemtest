
package service;

import domain.Account;
import domain.Customer;
import exceptions.InsufficientFundException;
import org.junit.jupiter.api.Test;
import repository.AccountRepository;
import repository.CustomerRepository;
import service.impl.BankServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {

    @Test
    void testMultipleSimultaneousWithdrawals() throws Exception {

        double initialBalance = 10000.0;
        List<Double> withdrawalAmounts = List.of(
                1000.0, 2500.0, 3000.0, 4000.0, 1500.0,
                2000.0, 500.0, 3500.0, 1000.0, 750.0
        );

        AccountRepository accountRepository = new AccountRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        BankServiceImpl bankService = new BankServiceImpl();
        String customerId = UUID.randomUUID().toString();

        Customer customer = new Customer(
                customerId,
                "Concurrency Test Customer",
                "test" + customerId + "@example.com"
        );

        customerRepository.save(customer);
        String accountNumber = "CT" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Account account = new Account(
                accountNumber,
                customerId,
                initialBalance,
                "SAVINGS"
        );

        accountRepository.save(account);

        System.out.println("\nStarting simultaneous withdrawal test...");
        System.out.println("Initial balance: Rs. " + initialBalance);
        System.out.println("Number of withdrawal requests: "
                + withdrawalAmounts.size());
        ExecutorService executor =
                Executors.newFixedThreadPool(withdrawalAmounts.size());

        CountDownLatch ready = new CountDownLatch(withdrawalAmounts.size());
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (Double amount : withdrawalAmounts) {

                Future<Boolean> result = executor.submit(() ->
                        makeWithdrawal(
                                bankService,
                                accountNumber,
                                amount,
                                ready,
                                start
                        )
                );

                results.add(result);
            }
            ready.await();

            System.out.println("All users are ready. Processing withdrawals...");
            start.countDown();

            int successfulWithdrawals = 0;
            int failedWithdrawals = 0;
            double totalWithdrawn = 0.0;
            for (int i = 0; i < results.size(); i++) {

                boolean success = results.get(i).get();

                if (success) {
                    successfulWithdrawals++;
                    totalWithdrawn += withdrawalAmounts.get(i);
                } else {
                    failedWithdrawals++;
                }
            }
            double finalBalance = accountRepository
                    .findByNumber(accountNumber)
                    .orElseThrow()
                    .getBalance();

            System.out.println("\nWithdrawal Results:");
            System.out.println("Successful withdrawals: "
                    + successfulWithdrawals);
            System.out.println("Failed withdrawals: "
                    + failedWithdrawals);
            System.out.println("Total amount withdrawn: Rs. "
                    + totalWithdrawn);
            System.out.println("Final account balance: Rs. "
                    + finalBalance);

            // Check that every request received a result
            assertEquals(
                    withdrawalAmounts.size(),
                    successfulWithdrawals + failedWithdrawals
            );
            assertTrue(finalBalance >= 0,
                    "Account balance can never be negative");
            assertEquals(
                    initialBalance,
                    totalWithdrawn + finalBalance,
                    0.01,
                    "Account balance calculation is incorrect"
            );

            assertTrue(failedWithdrawals > 0,
                    "Some withdrawals should fail due to insufficient funds");

            System.out.println("\nConcurrency test passed!");

        } finally {
            executor.shutdownNow();
        }
    }

    private boolean makeWithdrawal(
            BankServiceImpl bankService,
            String accountNumber,
            double amount,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {bankService.withdraw(accountNumber, amount, "Concurrent Withdrawal",UUID.randomUUID().toString());
            return true;

        } catch (RuntimeException e) {
            if (e.getCause() instanceof InsufficientFundException) {
                return false;
            }
            throw e;
        }
    }
}
