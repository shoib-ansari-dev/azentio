package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.detection.CurrencyConverter;
import com.customer.support.ai.appserver.detection.DetectionEngine;
import com.customer.support.ai.appserver.entity.Account;
import com.customer.support.ai.appserver.entity.Customer;
import com.customer.support.ai.appserver.entity.IngestionJob;
import com.customer.support.ai.appserver.entity.Transaction;
import com.customer.support.ai.appserver.exception.IngestionException;
import com.customer.support.ai.appserver.repository.AccountRepository;
import com.customer.support.ai.appserver.repository.CustomerRepository;
import com.customer.support.ai.appserver.repository.IngestionJobRepository;
import com.customer.support.ai.appserver.repository.TransactionRepository;
import com.opencsv.CSVReaderHeaderAware;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BulkIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BulkIngestionService.class);

    private final IngestionJobRepository jobRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyConverter currencyConverter;
    private final DetectionEngine detectionEngine;

    public BulkIngestionService(
            IngestionJobRepository jobRepository,
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CurrencyConverter currencyConverter,
            DetectionEngine detectionEngine) {
        this.jobRepository = jobRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.currencyConverter = currencyConverter;
        this.detectionEngine = detectionEngine;
    }

    public UUID submit(String jobType, MultipartFile file, String actor) {
        byte[] content = readBytes(file);
        UUID jobId = UUID.randomUUID();
        IngestionJob job = new IngestionJob();
        job.setId(jobId);
        job.setJobType(jobType);
        job.setStatus("QUEUED");
        job.setSubmittedBy(actor);
        jobRepository.save(job);
        process(jobType, jobId, content);
        return jobId;
    }

    @Async("ingestionExecutor")
    public void process(String jobType, UUID jobId, byte[] content) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("RUNNING");
        job.setStartedAt(OffsetDateTime.now());
        jobRepository.save(job);
        int processed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                new InputStreamReader(new java.io.ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                try {
                    ingestRow(jobType, jobId, row);
                    processed++;
                } catch (RuntimeException e) {
                    failed++;
                    if (errors.size() < 50) {
                        errors.add(e.getMessage());
                    }
                }
            }
            job.setTotalRecords(processed + failed);
            job.setProcessed(processed);
            job.setFailed(failed);
            job.setErrorSummary(errors.isEmpty() ? null : String.join("; ", errors));
            job.setStatus("COMPLETED");
        } catch (IOException | com.opencsv.exceptions.CsvValidationException e) {
            job.setStatus("FAILED");
            job.setErrorSummary(e.getMessage());
            log.error("Bulk ingestion failed for job {}", jobId, e);
        }
        job.setCompletedAt(OffsetDateTime.now());
        jobRepository.save(job);
    }

    private void ingestRow(String jobType, UUID jobId, Map<String, String> row) {
        switch (jobType) {
            case "CUSTOMERS" -> customerRepository.save(toCustomer(row));
            case "ACCOUNTS" -> accountRepository.save(toAccount(row));
            case "TRANSACTIONS" -> {
                Transaction transaction = transactionRepository.save(toTransaction(row, jobId));
                detectionEngine.evaluate(transaction);
            }
            default -> throw new IngestionException("Unknown job type " + jobType);
        }
    }

    private Customer toCustomer(Map<String, String> row) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setCustomerRef(req(row, "customer_id"));
        c.setFirstName(row.get("first_name"));
        c.setLastName(row.get("last_name"));
        c.setGender(row.get("gender"));
        c.setDateOfBirth(parseDate(row.get("date_of_birth")));
        c.setEmail(row.get("email"));
        c.setPhoneNumber(row.get("phone_number"));
        c.setCity(row.get("city"));
        c.setState(row.get("state"));
        c.setCountry(row.get("country"));
        c.setPostalCode(row.get("postal_code"));
        c.setOccupation(blankToNull(row.get("occupation")));
        c.setAnnualIncome(parseDecimal(row.get("annual_income")));
        c.setMaritalStatus(blankToNull(row.get("marital_status")));
        c.setEducationLevel(blankToNull(row.get("education_level")));
        c.setEmploymentStatus(blankToNull(row.get("employment_status")));
        c.setCustomerSince(parseDate(row.get("customer_since")));
        c.setCustomerSegment(row.get("customer_segment"));
        c.setKycStatus(row.get("kyc_status"));
        c.setRiskRating(row.get("risk_rating"));
        c.setPoliticallyExposed(parseBool(row.get("is_politically_exposed")));
        c.setPreferredChannel(row.get("preferred_channel"));
        c.setEmailVerified(parseBool(row.get("email_verified")));
        c.setPhoneVerified(parseBool(row.get("phone_verified")));
        c.setNumComplaintsLastYear(parseInt(row.get("num_complaints_last_year")));
        OffsetDateTime now = OffsetDateTime.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        return c;
    }

    private Account toAccount(Map<String, String> row) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setAccountRef(req(row, "account_id"));
        a.setCustomerId(resolveCustomerId(req(row, "customer_id")));
        a.setAccountType(row.get("account_type"));
        a.setAccountStatus(row.get("account_status"));
        a.setCurrency(row.get("currency"));
        a.setOpenDate(parseDate(row.get("open_date")));
        a.setCloseDate(parseDate(row.get("close_date")));
        a.setBranchCode(row.get("branch_code"));
        a.setBranchCity(row.get("branch_city"));
        a.setCurrentBalance(parseDecimal(row.get("current_balance")));
        a.setAvgMonthlyBalance6m(parseDecimal(row.get("avg_monthly_balance_6m")));
        a.setCreditLimit(parseDecimal(row.get("credit_limit")));
        a.setCreditUtilizationPct(parseDecimal(row.get("credit_utilization_pct")));
        a.setOverdraftEnabled(parseBool(row.get("overdraft_enabled")));
        a.setCardType(row.get("card_type"));
        a.setJointAccount(parseBool(row.get("is_joint_account")));
        a.setNumLinkedDevices(parseInt(row.get("num_linked_devices")));
        a.setMobileBankingEnrolled(parseBool(row.get("mobile_banking_enrolled")));
        a.setLastLoginDate(parseDate(row.get("last_login_date")));
        a.setAvgMonthlyTxnCount(parseInt(row.get("avg_monthly_txn_count")));
        a.setAccountTier(row.get("account_tier"));
        a.setRiskRating(blankToNull(row.get("risk_rating")));
        OffsetDateTime now = OffsetDateTime.now();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        return a;
    }

    private Transaction toTransaction(Map<String, String> row, UUID jobId) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setTransactionRef(req(row, "transaction_ref"));
        t.setAccountId(resolveAccountId(req(row, "account_id")));
        BigDecimal amount = parseDecimal(req(row, "amount"));
        String currency = req(row, "currency");
        t.setAmount(amount);
        t.setCurrency(currency);
        CurrencyConverter.ConversionResult conversion = currencyConverter.toInr(currency, amount);
        t.setAmountInr(conversion.amountInr());
        t.setExchangeRateUsed(conversion.rateUsed());
        t.setTransactionType(row.get("transaction_type"));
        t.setChannel(row.get("channel"));
        t.setCounterpartyAccount(blankToNull(row.get("counterparty_account")));
        t.setCounterpartyBank(blankToNull(row.get("counterparty_bank")));
        t.setCounterpartyJurisdiction(blankToNull(row.get("counterparty_jurisdiction")));
        t.setDescription(row.get("description"));
        t.setTransactionTimestamp(parseTimestamp(req(row, "transaction_timestamp")));
        t.setIngestedAt(OffsetDateTime.now());
        t.setStatus("PROCESSED");
        t.setJobId(jobId);
        return t;
    }

    private UUID resolveCustomerId(String customerRef) {
        return customerRepository.findByCustomerRef(customerRef)
                .orElseThrow(() -> new IngestionException("Unknown customer_id " + customerRef))
                .getId();
    }

    private UUID resolveAccountId(String accountRef) {
        return accountRepository.findByAccountRef(accountRef)
                .orElseThrow(() -> new IngestionException("Unknown account_id " + accountRef))
                .getId();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IngestionException("Could not read uploaded file");
        }
    }

    private static String req(Map<String, String> row, String key) {
        String value = blankToNull(row.get(key));
        if (value == null) {
            throw new IngestionException("Missing required column " + key);
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDate parseDate(String value) {
        String v = blankToNull(value);
        return v == null ? null : LocalDate.parse(v);
    }

    private static OffsetDateTime parseTimestamp(String value) {
        return OffsetDateTime.parse(value);
    }

    private static BigDecimal parseDecimal(String value) {
        String v = blankToNull(value);
        return v == null ? null : new BigDecimal(v);
    }

    private static Integer parseInt(String value) {
        String v = blankToNull(value);
        return v == null ? null : Integer.valueOf(v);
    }

    private static boolean parseBool(String value) {
        String v = blankToNull(value);
        return "Y".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v) || "1".equals(v);
    }
}
