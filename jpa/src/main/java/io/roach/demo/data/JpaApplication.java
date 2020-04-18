package io.roach.demo.data;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.*;

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
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@EnableAutoConfiguration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableJpaRepositories
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableSpringDataWebSupport
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1)
@Configuration
@ComponentScan
public class JpaApplication implements WebMvcConfigurer {
    protected static final Logger logger = LoggerFactory.getLogger(JpaApplication.class);

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new FormHttpMessageConverter());
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(JpaApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(args);

        testClient();
    }

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

@Entity
@Table(name = "account")
class AccountEntity {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false, unique = true)
    private String name;

    @Column(length = 25, nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Column(length = 25, nullable = false)
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

@Repository
@Transactional(propagation = MANDATORY)
interface AccountRepository extends JpaRepository<AccountEntity, Long>,
        JpaSpecificationExecutor<AccountEntity> {

    @Query(value = "select balance from AccountEntity where id=?1")
    BigDecimal getBalance(Long id);

    @Modifying
    @Query("update AccountEntity set balance = balance + ?2 where id=?1")
    void updateBalance(Long id, BigDecimal balance);
}

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Negative balance")
class NegativeBalanceException extends DataIntegrityViolationException {
    public NegativeBalanceException(String message) {
        super(message);
    }
}

@RestController
class AccountController {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PagedResourcesAssembler<AccountEntity> pagedResourcesAssembler;

    @GetMapping
    public ResponseEntity<RepresentationModel> index() {
        RepresentationModel index = new RepresentationModel();

        index.add(linkTo(methodOn(AccountController.class)
                .listAccounts(PageRequest.of(0, 5)))
                .withRel("accounts"));

        index.add(linkTo(AccountController.class)
                .slash("transfer{?fromId,toId,amount}")
                .withRel("transfer"));

        return new ResponseEntity<>(index, HttpStatus.OK);
    }

    @GetMapping("/account")
    @Transactional(propagation = REQUIRES_NEW)
    public HttpEntity<PagedModel<AccountModel>> listAccounts(
            @PageableDefault(size = 5, direction = Sort.Direction.ASC) Pageable page) {
        return ResponseEntity
                .ok(pagedResourcesAssembler.toModel(accountRepository.findAll(page), accountModelAssembler()));
    }

    @GetMapping(value = "/account/{id}")
    @Transactional(propagation = REQUIRES_NEW)
    public HttpEntity<AccountModel> getAccount(@PathVariable("id") Long accountId) {
        return new ResponseEntity<>(accountModelAssembler().toModel(accountRepository.getOne(accountId)),
                HttpStatus.OK);
    }

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

        if (fromBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeBalanceException("Insufficient funds " + amount + " for account " + fromId);
        }

        accountRepository.updateBalance(fromId, amount.negate());
        accountRepository.updateBalance(toId, amount);

        return ResponseEntity.ok().build();
    }

    private RepresentationModelAssembler<AccountEntity, AccountModel> accountModelAssembler() {
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

@Component
@Aspect
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
            } catch (TransientDataAccessException | TransactionSystemException | JpaSystemException ex) {
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
