package io.roach.demo.data;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.session.SqlSessionFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.*;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.MyBatisJdbcConfiguration;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
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

import lombok.Data;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@EnableAutoConfiguration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableJdbcRepositories
@EnableSpringDataWebSupport
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1)
@Configuration
@ComponentScan
@Import(MyBatisJdbcConfiguration.class)
public class MyBatisApplication implements WebMvcConfigurer {
    protected static final Logger logger = LoggerFactory.getLogger(MyBatisApplication.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new FormHttpMessageConverter());
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(MyBatisApplication.class)
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

@Data
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

interface PagedAccountRepository {
    Page<Account> findAll(Pageable pageable);
}

@Mapper
interface PagedAccountMapper {
    @Select("SELECT * FROM account LIMIT #{pageable.pageSize} OFFSET #{pageable.offset}")
    List<Account> findAll(@Param("pageable") Pageable pageable);

    @Select("SELECT count(id) from account")
    long countAll();
}

@Repository
@Transactional(propagation = MANDATORY)
class PagedAccountRepositoryImpl implements PagedAccountRepository {
    @Autowired
    private PagedAccountMapper pagedAccountMapper;

    @Override
    public Page<Account> findAll(Pageable pageable) {
        List<Account> accounts = pagedAccountMapper.findAll(pageable);
        long totalRecords = pagedAccountMapper.countAll();
        return new PageImpl<>(accounts, pageable, totalRecords);

    }
}

@Repository
@Transactional(propagation = MANDATORY)
interface AccountRepository extends CrudRepository<Account, Long>, PagedAccountRepository {
    @Query("SELECT balance FROM account WHERE id = :id")
    BigDecimal getBalance(@Param("id") Long id);

    @Query("UPDATE Account set balance = balance + :balance where id=:id")
    @Modifying
    void updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Negative balance")
class NegativeBalanceException extends DataIntegrityViolationException {
    public NegativeBalanceException(String message) {
        super(message);
    }
}

class IndexModel extends RepresentationModel<IndexModel> {
    private String message;

    public IndexModel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

@RestController
class AccountController {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PagedResourcesAssembler<Account> pagedResourcesAssembler;

    @GetMapping
    public ResponseEntity<IndexModel> index() {
        IndexModel index = new IndexModel("Hello Spring Boot + CockroachDB + MyBatis");

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
        return new ResponseEntity<>(accountModelAssembler().toModel(accountRepository.findById(accountId)
                .orElseThrow(() -> new DataRetrievalFailureException("No such account: " + accountId))),
                HttpStatus.OK);
    }

    @GetMapping(value = "/account/{id}/balance")
    @Transactional(propagation = REQUIRES_NEW)
    public HttpEntity<String> getAccountBalance(@PathVariable("id") Long accountId) {
        return new ResponseEntity<>(accountRepository.getBalance(accountId).toString(), HttpStatus.OK);
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

    private RepresentationModelAssembler<Account, AccountModel> accountModelAssembler() {
        return (entity) -> {
            AccountModel model = new AccountModel();
            model.setName(entity.getName());
            model.setType(entity.getType());
            model.setBalance(entity.getBalance());
            model.add(linkTo(methodOn(AccountController.class)
                    .getAccount(entity.getId())
            ).withRel(IanaLinkRelations.SELF));
            model.add(linkTo(methodOn(AccountController.class)
                    .getAccountBalance(entity.getId())
            ).withRel("balance"));
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
