package app;

import domain.Account;
import domain.Customer;
import exceptions.InsufficientFundException;
import repository.AccountRepository;
import repository.CustomerRepository;
import service.impl.BankServiceImpl;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class PerformanceBenchmark {
    private static final int TOTAL_TRANSACTIONS = 5000;
    private static final double INITIAL_BALANCE = 2500.0;
    private static final double WITHDRAWAL_AMOUNT = 1.0;
    public static void main(String args[])throws Exception{
        int []workerCounts={1,5,10,20};
        System.out.println(" Banking System Performance Test :");
        System.out.println("Total transactions: "+TOTAL_TRANSACTIONS);
        for (int workers : workerCounts) {
            runBenchmark(workers);
        }
    }

    private static void runBenchmark(int workerCount) throws Exception {
        AccountRepository accountRepository = new AccountRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        BankServiceImpl bankService = new BankServiceImpl();
        String customerId = UUID.randomUUID().toString();
        Customer customer = new Customer(
                customerId,
                "Benchmark Customer",
                "benchmark" + customerId + "@example.com"
        );

        customerRepository.save(customer);

        String accountNumber = "BM" +
                UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0, 12);

        Account account = new Account(
                accountNumber,
                customerId,
                INITIAL_BALANCE,
                "SAVINGS"
        );

        accountRepository.save(account);

        AtomicInteger nextTransaction = new AtomicInteger(0);
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        ExecutorService executor =
                Executors.newFixedThreadPool(workerCount);

        CountDownLatch start = new CountDownLatch(1);

        System.out.println("\nTesting with " + workerCount + " workers...");

        long startTime = System.nanoTime();

        try {

            for (int i = 0; i < workerCount; i++) {

                executor.submit(() -> {

                    try {
                        start.await();

                        while (true) {

                            int transactionNumber =
                                    nextTransaction.getAndIncrement();

                            if (transactionNumber >= TOTAL_TRANSACTIONS) {
                                break;
                            }

                            try {bankService.withdraw(accountNumber, WITHDRAWAL_AMOUNT, "Benchmark Withdrawal",UUID.randomUUID().toString());
                                successful.incrementAndGet();
                            } catch (RuntimeException e) {

                                if (e.getCause()
                                        instanceof InsufficientFundException) {

                                    failed.incrementAndGet();

                                } else {
                                    throw e;
                                }
                            }
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                });
            }
            start.countDown();

            executor.shutdown();

            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                throw new RuntimeException("Benchmark timed out");
            }

        } finally {
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        }

        long endTime = System.nanoTime();

        double executionTime =
                (endTime - startTime) / 1_000_000_000.0;

        double transactionsPerSecond =
                TOTAL_TRANSACTIONS / executionTime;

        double finalBalance = accountRepository
                .findByNumber(accountNumber)
                .orElseThrow()
                .getBalance();

        System.out.println("Successful transactions: " + successful.get());
        System.out.println("Failed transactions: " + failed.get());
        System.out.println("Final balance: Rs. " + finalBalance);
        System.out.printf("Execution time: %.2f seconds%n", executionTime);
        System.out.printf("Transactions per second: %.2f%n",
                transactionsPerSecond);

        if (successful.get() + failed.get() != TOTAL_TRANSACTIONS) {
            throw new RuntimeException("Transaction count mismatch");
        }

        if (finalBalance < 0 ||
                Math.abs(INITIAL_BALANCE -
                        (successful.get() * WITHDRAWAL_AMOUNT + finalBalance))
                        > 0.01) {
            throw new RuntimeException("Account balance verification failed");
        }
    }
}