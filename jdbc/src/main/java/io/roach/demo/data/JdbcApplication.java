package io.roach.demo.data;

import java.lang.annotation.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.*;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Spring boot server application using spring-data-jdbc for data access.
 */
@EnableAutoConfiguration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableJdbcRepositories
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableSpringDataWebSupport
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1) // Bump up one level to enable extra advisors
@Configuration
@ComponentScan
public class JdbcApplication extends AbstractJdbcConfiguration implements WebMvcConfigurer {
    protected static final Logger logger = LoggerFactory.getLogger(JdbcApplication.class);

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new FormHttpMessageConverter());
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(JdbcApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(args);

        for (String a : args) {
            if ("--skip-client".equals(a)) {
                return;
            }
        }

        testClient();
    }

    /**
     * Client logic that submits money transfer requests to the REST API.
     */
    public static void testClient() {
        logger.info("Lets move some $$ around!");

        final Link transferLink = new Link("http://localhost:8080/transfer{?fromId,toId,amount}");

        final int threads = Runtime.getRuntime().availableProcessors();

        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(threads);

        Deque<Future<?>> futures = new ArrayDeque<>();

        for (int i = 0; i < threads; i++) {
            Future<?> future = executorService.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    int fromId = 1 + (int) Math.round(Math.random() * 3);
                    int toId = fromId % 4 + 1;

                    BigDecimal amount = new BigDecimal("10.00");

                    Map<String, Object> form = new HashMap<>();
                    form.put("fromId", fromId);
                    form.put("toId", toId);
                    form.put("amount", amount);

                    String uri = transferLink.expand(form).getHref();

                    try {
                        new RestTemplate().exchange(uri, HttpMethod.POST, new HttpEntity<>(null), String.class);
                    } catch (HttpClientErrorException.BadRequest e) {
                        logger.warn(e.getResponseBodyAsString());
                    }
                }
            });
            futures.add(future);
        }

        while (!futures.isEmpty()) {
            try {
                futures.pop().get();
                logger.info("Worker finished - {} remaining", futures.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logger.warn("Worker failed", e.getCause());
            }
        }

        logger.info("All client workers finished but server keeps running. Have a nice day!");

        executorService.shutdownNow();
    }
}

enum AccountType {
    asset,
    expense
}

/**
 * Domain entity mapped to the account table.
 */
class Account {
    @Id
    private Long id;

    private String name;

    private AccountType type;

    private BigDecimal balance;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}

/**
 * Account resource represented in HAL+JSON via REST API.
 */
@Relation(value = "account", collectionRelation = "accounts")
class AccountModel extends RepresentationModel<AccountModel> {
    private String name;

    private AccountType type;

    private BigDecimal balance;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

/**
 * Pagination is not available in spring-data-jdbc (yet) so we create a separate
 * repository to provide basic limit+offset pagination queries for accounts.
 */
interface PagedAccountRepository {
    Page<Account> findAll(Pageable pageable);
}

@Repository
interface PagedAccountHelper extends org.springframework.data.repository.Repository<Account, Long> {
    @Query("SELECT * FROM account LIMIT :pageSize OFFSET :offset")
    List<Account> findAll(@Param("pageSize") int pageSize, @Param("offset") long offset);

    @Query("SELECT count(id) FROM account")
    long countAll();
}

@Repository
// @Transactional is not needed but here for clarity since we want repos to always be called from a tx context
@Transactional(propagation = MANDATORY)
class PagedAccountRepositoryImpl implements PagedAccountRepository {
    @Autowired
    private PagedAccountHelper pagedAccountHelper;

    @Override
    public Page<Account> findAll(Pageable pageable) {
        return new PageImpl<>(pagedAccountHelper.findAll(pageable.getPageSize(), pageable.getOffset()),
                pageable,
                pagedAccountHelper.countAll());
    }
}

/**
 * The main account repository, notice there's no implementation needed since its auto-proxied by
 * spring-data.
 * <p>
 * Should have extended PagingAndSortingRepository in normal cases.
 */
@Repository
@Transactional(propagation = MANDATORY)
interface AccountRepository extends CrudRepository<Account, Long>, PagedAccountRepository {
    @Query(value = "SELECT balance FROM account WHERE id=:id")
    BigDecimal getBalance(@Param("id") Long id);

    @Modifying
    @Query("UPDATE account SET balance = balance + :balance WHERE id=:id")
    void updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}

/**
 * Business exception that maps to a given HTTP status code.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Negative balance")
class NegativeBalanceException extends DataIntegrityViolationException {
    public NegativeBalanceException(String message) {
        super(message);
    }
}

/**
 * Annotation marking a transaction boundary to use follower reads (time travel).
 * See https://www.cockroachlabs.com/docs/stable/follower-reads.html
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@interface TimeTravel {
    int value() default -1; // Non-zero denotes follower read
}

/**
 * Main remoting and transaction boundary in the form of a REST controller. The discipline
 * when following the entity-control-boundary (ECB) pattern is that only service boundaries
 * are allowed to start and end transactions. A service boundary can be a controller, business
 * service facade or service activator (JMS/Kafka listener).
 * <p>
 * This is enforced by the REQUIRES_NEW propagation attribute of @Transactional annotated
 * controller methods. Between the web container's HTTP listener and the transaction proxy,
 * there's yet another transparent proxy in the form of a retry loop advice with exponential
 * backoff. It takes care of retrying transactions that are aborted by transient SQL errors,
 * rather than having these propagate all the way over the wire to the client / user agent.
 *
 * @see RetryableTransactionAspect
 */
@RestController
class AccountController {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PagedResourcesAssembler<Account> pagedResourcesAssembler;

    /**
     * Provides the service index resource representation which is only links
     * for clients to follow.
     */
    @GetMapping
    public ResponseEntity<RepresentationModel> index() {
        RepresentationModel index = new RepresentationModel();

        // Type-safe way to generate URLs bound to controller methods
        index.add(linkTo(methodOn(AccountController.class)
                .listAccounts(PageRequest.of(0, 5)))
                .withRel("accounts")); // Lets skip curies and affordances for now

        // This rel essentially informs the client that a POST to its href with
        // form parameters will transfer funds between referenced accounts.
        // (its only a demo)
        index.add(linkTo(AccountController.class)
                .slash("transfer{?fromId,toId,amount}")
                .withRel("transfer"));

        // Spring boot actuators for observability / monitoring
        index.add(new Link(
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .pathSegment("actuator")
                        .buildAndExpand()
                        .toUriString()
        ).withRel("actuator"));

        return new ResponseEntity<>(index, HttpStatus.OK);
    }

    /**
     * Provides a paged representation of accounts (sort order omitted).
     */
    @GetMapping("/account")
    @Transactional(propagation = REQUIRES_NEW)
    @TimeTravel // We dont need the result to be authoritative, so any follower replica can service the read
    public HttpEntity<PagedModel<AccountModel>> listAccounts(
            @PageableDefault(size = 5, direction = Sort.Direction.ASC) Pageable page) {
        return ResponseEntity
                .ok(pagedResourcesAssembler.toModel(accountRepository.findAll(page), accountModelAssembler()));
    }

    /**
     * Provides a point lookup of a given account.
     */
    @GetMapping(value = "/account/{id}")
    @Transactional(propagation = REQUIRES_NEW, readOnly = true) // Notice its marked read-only
    public HttpEntity<AccountModel> getAccount(@PathVariable("id") Long accountId) {
        return new ResponseEntity<>(accountModelAssembler().toModel(
                accountRepository.findById(accountId)
                        .orElseThrow(() -> new DataRetrievalFailureException("No such account: " + accountId))),
                HttpStatus.OK);
    }

    /**
     * Main funds transfer method.
     */
    @PostMapping(value = "/transfer")
    @Transactional(propagation = REQUIRES_NEW)
    public HttpEntity<BigDecimal> transfer(
            @RequestParam("fromId") Long fromId,
            @RequestParam("toId") Long toId,
            @RequestParam("amount") BigDecimal amount
    ) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative amount");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("From and to accounts must be different");
        }

        BigDecimal fromBalance = accountRepository.getBalance(fromId).add(amount.negate());
        // Application level invariant check.
        // Could be enhanced or replaced with a CHECK constraint like:
        // ALTER TABLE account ADD CONSTRAINT check_account_positive_balance CHECK (balance >= 0)
        if (fromBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeBalanceException("Insufficient funds " + amount + " for account " + fromId);
        }

        accountRepository.updateBalance(fromId, amount.negate());
        accountRepository.updateBalance(toId, amount);

        return ResponseEntity.ok().build();
    }

    private RepresentationModelAssembler<Account, AccountModel> accountModelAssembler() {
        return (entity) -> {
            AccountModel model = new AccountModel();
            model.setName(entity.getName());
            model.setType(entity.getType());
            model.setBalance(entity.getBalance());
            model.add(linkTo(methodOn(AccountController.class)
                    .getAccount(entity.getId())
            ).withRel(IanaLinkRelations.SELF));
            return model;
        };
    }
}

/**
 * Aspect with an around advice that intercepts and retries transient concurrency exceptions.
 * Methods matching the pointcut expression (annotated with @Transactional) are retried a number
 * of times with exponential backoff.
 * <p>
 * This advice needs to runs in a non-transactional context, which is before the underlying
 * transaction advisor (@Order ensures that).
 */
@Component
@Aspect
// Before TX advisor
@Order(Ordered.LOWEST_PRECEDENCE - 2)
class RetryableTransactionAspect {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("execution(* io.roach..*(..)) && @annotation(transactional)")
    public void anyTransactionBoundaryOperation(Transactional transactional) {
    }

    @Around(value = "anyTransactionBoundaryOperation(transactional)",
            argNames = "pjp,transactional")
    public Object retryableOperation(ProceedingJoinPoint pjp, Transactional transactional)
            throws Throwable {
        final int totalRetries = 30;
        int numAttempts = 0;
        AtomicLong backoffMillis = new AtomicLong(150);

        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(), "TX active");

        do {
            try {
                numAttempts++;
                return pjp.proceed();
            } catch (TransientDataAccessException | TransactionSystemException ex) {
                handleTransientException(ex, numAttempts, totalRetries, pjp, backoffMillis);
            } catch (UndeclaredThrowableException ex) {
                Throwable t = ex.getUndeclaredThrowable();
                if (t instanceof TransientDataAccessException) {
                    handleTransientException(t, numAttempts, totalRetries, pjp, backoffMillis);
                } else {
                    throw ex;
                }
            }
        } while (numAttempts < totalRetries);

        throw new ConcurrencyFailureException("Too many transient errors (" + numAttempts + ") for method ["
                + pjp.getSignature().toLongString() + "]. Giving up!");
    }

    private void handleTransientException(Throwable ex, int numAttempts, int totalAttempts,
                                          ProceedingJoinPoint pjp, AtomicLong backoffMillis) {
        if (logger.isWarnEnabled()) {
            logger.warn("Transient data access exception (" + numAttempts + " of max " + totalAttempts + ") "
                    + "detected (retry in " + backoffMillis + " ms) "
                    + "in method '" + pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName()
                    + "': " + ex.getMessage());
        }
        if (backoffMillis.get() >= 0) {
            try {
                Thread.sleep(backoffMillis.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            backoffMillis.set(Math.min((long) (backoffMillis.get() * 1.5), 1500));
        }
    }
}

/**
 * Aspect with an around advice that intercepts and sets transaction attributes.
 * <p>
 * This advice needs to runs in a transactional context, which is after the underlying
 * transaction advisor.
 */
@Component
@Aspect
// After TX advisor
@Order(Ordered.LOWEST_PRECEDENCE)
class TransactionHintsAspect {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String applicationName = "roach-demo-data";

    @Pointcut("execution(* io.roach..*(..)) && @annotation(transactional)")
    public void anyTransactionBoundaryOperation(Transactional transactional) {
    }

    @Pointcut("execution(* io.roach..*(..)) && @annotation(followerRead)")
    public void anyFollowerReadOperation(TimeTravel followerRead) {
    }

    @Around(value = "anyTransactionBoundaryOperation(transactional)",
            argNames = "pjp,transactional")
    public Object setTransactionAttributes(ProceedingJoinPoint pjp, Transactional transactional)
            throws Throwable {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "TX not active");

        // https://www.cockroachlabs.com/docs/v19.2/set-vars.html
        jdbcTemplate.update("SET application_name=?", applicationName);

        if (transactional.timeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
            logger.info("Setting statement time {} for {}", transactional.timeout(),
                    pjp.getSignature().toShortString());
            jdbcTemplate.update("SET statement_timeout=?", transactional.timeout() * 1000);
        }

        if (transactional.readOnly()) {
            logger.info("Setting transaction read only for {}", pjp.getSignature().toShortString());
            jdbcTemplate.execute("SET transaction_read_only=true");
        }

        return pjp.proceed();
    }

    @Around(value = "anyFollowerReadOperation(timeTravel)",
            argNames = "pjp,timeTravel")
    public Object setTimeTravelAttributes(ProceedingJoinPoint pjp, TimeTravel timeTravel)
            throws Throwable {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "TX not active");

        logger.info("Providing {} via follower read", pjp.getSignature().toShortString());

        if (timeTravel.value() >= 0) {
            jdbcTemplate.update("SET TRANSACTION AS OF SYSTEM TIME INTERVAL '" + timeTravel.value() + "'");
        } else {
            jdbcTemplate.execute("SET TRANSACTION AS OF SYSTEM TIME experimental_follower_read_timestamp()");
        }

        return pjp.proceed();
    }
}
