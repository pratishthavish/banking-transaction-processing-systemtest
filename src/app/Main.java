package app;
import service.BankService;
import service.impl.BankServiceImpl;

import java.util.*;
public class Main {
    public static void main(String args[]){
        Scanner sc=new Scanner(System.in);
        BankService bs=new BankServiceImpl() ;
        System.out.println("Welcome to NG Bank");
        boolean run=true;
        while(run) {
            System.out.println("""
                    1)Create
                    2)Deposit
                    3)Withdraw
                    4)Transfer
                    5)Account Statement
                    6)List Accounts
                    7)Search Accounts by Customer Name
                    0)Exit
                    """);
            System.out.println("Choice : ");
            String choice = sc.nextLine().trim();
            System.out.println("Choice : " + choice);
            switch(choice){
                case "1"-> openAccount(sc, bs);
                case "2"-> deposit(sc,bs);
                case "3"-> withdraw(sc,bs);
                case "4"-> transfer(sc,bs);
                case "5"-> statemanet(sc,bs);
                case "6"-> listAccounts(sc,bs);
                case "7"-> searchAccounts(sc,bs);
                case "0"-> run=false;
            }
        }
    }

    private static void openAccount(Scanner sc,BankService bs) {
        System.out.println("Customer Name: ");
        String name=sc.nextLine().trim();
        System.out.println("Customer Email: ");
        String email=sc.nextLine().trim();
        System.out.println("Account Type (Savings/Current): ");
        String type=sc.nextLine().trim();
        System.out.println("Initial deposit (optional , blank for 0): ");
        String amountStr=sc.nextLine().trim();
        if(amountStr.isBlank())amountStr="0";
        Double initial=Double.valueOf(amountStr);
        String accountNumber = bs.openAccount(name, email, type);
        if(initial>0)
            bs.deposit(accountNumber,initial,"Initial Deposit", UUID.randomUUID().toString());
        System.out.println("Account opened: "+accountNumber);
    }

    private static void deposit(Scanner sc ,BankService bs) {
        System.out.println("Account number: ");
        String accountNumber=sc.nextLine().trim();
        System.out.println("Amount: ");
        Double amount=Double.valueOf(sc.nextLine().trim());
        bs.deposit(accountNumber,amount,"Deposit", UUID.randomUUID().toString());
        System.out.println("Deposited");
    }

    private static void withdraw(Scanner sc,BankService bs) {
        System.out.println("Account number: ");
        String accountNumber=sc.nextLine().trim();
        System.out.println("Amount: ");
        Double amount=Double.valueOf(sc.nextLine().trim());
        bs.withdraw(accountNumber,amount,"Withdrawal", UUID.randomUUID().toString());
        System.out.println("Withdrawn");
    }

    private static void transfer(Scanner sc,BankService bs) {
        System.out.println("From Account : ");
        String from=sc.nextLine().trim();
        System.out.println("To Account: ");
        String to=sc.nextLine().trim();
        System.out.println("Amount: ");
        Double amount=Double.valueOf(sc.nextLine().trim());
        bs.transfer(from,to,amount,"Transfer", UUID.randomUUID().toString());

    }

    private static void statemanet(Scanner sc,BankService bs) {
        System.out.println("Account Number : ");
        String account=sc.nextLine().trim();
        bs.getStatement(account).forEach(t->{
            System.out.println(t.getTimestamp()+"\t|\t"+t.getType()+"\t|\t"+t.getAmount()+"\t|\t"+t.getNote());
        });
    }


    private static void listAccounts(Scanner sc,BankService bs) {
        bs.listAccounts().forEach(a->{
            System.out.println(a.getAccountNo()+"\t|\t"+a.getAccountType()+"\t|\t"+a.getBalance());
        });
    }

    private static void searchAccounts(Scanner sc,BankService bs) {
        System.out.println("Customer name contains: ");
        String q=sc.nextLine().trim();
        bs.searchAccountByCustomerName(q).forEach(a->
                System.out.println(a.getAccountNo()+"\t|\t"+a.getAccountType()+"\t|\t"+a.getBalance())
        );
    }
}
