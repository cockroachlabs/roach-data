package io.roach.data.reactive;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@RestController
public class AccountController {
    @Autowired
    private AccountRepository accountRepository;

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
    public Mono<Page<Account>> listAccounts(@PageableDefault(size = 5, direction = Sort.Direction.ASC) Pageable page) {
        return getAccounts(PageRequest.of(page.getPageNumber(), page.getPageSize()));
    }

    private Mono<Page<Account>> getAccounts(PageRequest pageRequest) {
        return this.accountRepository.findAllBy(pageRequest)
                .collectList()
                .zipWith(this.accountRepository.count())
                .map(t -> new PageImpl<>(t.getT1(), pageRequest, t.getT2()));
    }

    @GetMapping(value = "/account/{id}")
    @Transactional(propagation = REQUIRES_NEW)
    public Mono<ResponseEntity<Account>> getAccount(@PathVariable("id") Long accountId) {
        return accountRepository.findById(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/transfer")
    @Transactional(propagation = REQUIRES_NEW)
    public Mono<Void> transfer(
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

        return accountRepository.getBalance(fromId).<BigDecimal>handle((balance, sink) -> {
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                sink.error(new NegativeBalanceException("Insufficient funds " + amount + " for account " + fromId));
                return;
            }
            sink.next(balance);
        }).flatMap(unused -> accountRepository.updateBalance(fromId, amount.negate()))
                .then(Mono.defer(() -> accountRepository.updateBalance(toId, amount)));
    }
}
