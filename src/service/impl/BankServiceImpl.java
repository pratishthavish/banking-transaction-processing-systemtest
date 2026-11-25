package service.impl;
import domain.Account;
import domain.Customer;
import domain.Transaction;
import domain.Type;
import exceptions.AccountNotFoundException;
import exceptions.InsufficientFundException;
import exceptions.ValidationException;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import service.BankService;
import util.Validation;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class BankServiceImpl implements BankService{

private final AccountRepository accountRepository=new AccountRepository();
private final TransactionRepository transactionRepository=new TransactionRepository();
private final CustomerRepository customerRepository=new CustomerRepository();

private final Validation<String>validateName=name->{
    if(name==null|| name.isBlank())throw new ValidationException("Name is required");
};
private final Validation<String>validateEmail=email->{
    if(email==null|| !email.contains("@"))throw new ValidationException("Email is required");
};
private final Validation<String>validateType=type->{
    if(type==null|| !(type.equalsIgnoreCase("SAVINGS")|| type.equalsIgnoreCase("CURRENT")))throw new ValidationException("Account Type must be SAVINGS OR CURRENT");
};
private final Validation<Double>validateAmountPositive=amount->{
    if(amount==null|| amount<0)throw new ValidationException("Please enter vaild amount");
};


    @Override
    public String openAccount(String name, String email, String accountType) {

      validateName.validate(name);
      validateEmail.validate(email);
      validateType.validate(accountType);

        String customerId= UUID.randomUUID().toString();
       //String accountNumber= UUID.randomUUID().toString();
        Customer c=new Customer(customerId,name,email);
        customerRepository.save(c);
        String accountNumber = getAccountNumber();

        Account a = new Account(accountNumber, customerId, (double)0, accountType);
        //save
        accountRepository.save(a);
        return accountNumber;
    }

    @Override
    public List<Account> listAccounts() {
        return accountRepository.findAll().stream().sorted(Comparator.comparing(Account::getAccountNo)).collect(Collectors.toList());
    }

    @Override
    public void deposit(String accountNumber, Double amount, String note) {
        validateAmountPositive.validate(amount);
        Account account=accountRepository.findByNumber(accountNumber)
                .orElseThrow(()-> new AccountNotFoundException( "Account not found: "+accountNumber));
        account.setBalance(account.getBalance()+amount);
        Transaction transaction=new Transaction(account.getAccountNo(),amount,UUID.randomUUID().toString(), LocalDateTime.now(),note, Type.DEPOSIT);
        transactionRepository.add(transaction);
    }

    @Override
    public void withdraw(String accountNumber, Double amount, String withdrawal) {
        Account account=accountRepository.findByNumber(accountNumber).orElseThrow(()-> new RuntimeException( "Account not found: "+accountNumber));
        if(account.getBalance().compareTo(amount)<0)
            throw new InsufficientFundException("insufficient Balance");
        account.setBalance(account.getBalance()-amount);
        Transaction transaction=new Transaction(account.getAccountNo(),amount,UUID.randomUUID().toString(), LocalDateTime.now(),withdrawal, Type.WITHDRAW);
        transactionRepository.add(transaction);
    }

    @Override
    public void transfer(String fromAcc, String toAcc, Double amount, String note ) {
        if(fromAcc.equals(toAcc)){
            throw new ValidationException("Cannot transfer to your own account");
        }
        Account from=accountRepository.findByNumber(fromAcc)
                .orElseThrow(()-> new AccountNotFoundException( "Account not found: "+fromAcc));
        Account to=accountRepository.findByNumber(toAcc)
                .orElseThrow(()-> new AccountNotFoundException( "Account not found: "+toAcc));
        if(from.getBalance().compareTo(amount)<0)
            throw new InsufficientFundException("Insufficient Balance");
        from.setBalance(from.getBalance()-amount);
        to.setBalance(to.getBalance()+amount);
        transactionRepository.add(new Transaction(from.getAccountNo(),amount,UUID.randomUUID().toString(), LocalDateTime.now(),note, Type.TRANSFER_OUT));
        transactionRepository.add(new Transaction(to.getAccountNo(),amount,UUID.randomUUID().toString(), LocalDateTime.now(),note, Type.TRANSFER_IN));
    }

    @Override
    public List<Transaction> getStatement(String account) {
        return transactionRepository.findByAccount(account).stream().sorted(Comparator.comparing(Transaction::getTimestamp)).collect(Collectors.toList());
    }

    @Override
    public List<Account> searchAccountByCustomerName(String q) {
        String query=(q==null)?"":q.toLowerCase();
       /* List<Account> result=new ArrayList<>();
        for (Customer c: customerRepository.findAll()){
            if(c.getName().toLowerCase().contains(query))
                result.addAll(accountRepository.findByCustomerId(c.getId()));

        }
        result.sort(Comparator.comparing(Account::getAccountNo));
        return result;*/
        return customerRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(query))
                .flatMap(c -> accountRepository
                        .findByCustomerId(c.getId())
                        .stream())
                .sorted(Comparator.comparing(Account::getAccountNo))
                .collect(Collectors.toList());
    }


    private String getAccountNumber() {
        int size=accountRepository.findAll().size()+1;
        return String.format("AC%06d",size);
    }
}
