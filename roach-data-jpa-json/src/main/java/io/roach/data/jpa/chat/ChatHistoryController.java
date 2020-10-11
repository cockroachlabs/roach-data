package io.roach.data.jpa.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@RestController
public class ChatHistoryController {
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private PagedResourcesAssembler<ChatHistory> pagedResourcesAssembler;

    @GetMapping
    public ResponseEntity<RepresentationModel<?>> index() {
        RepresentationModel<?> index = new RepresentationModel<>();

        index.add(linkTo(methodOn(ChatHistoryController.class)
                .listChats(PageRequest.of(0, 5)))
                .withRel("chats"));

        return new ResponseEntity<>(index, HttpStatus.OK);
    }

    @GetMapping("/chathistory")
    @Transactional(propagation = REQUIRES_NEW)
    public HttpEntity<PagedModel<EntityModel<ChatHistory>>> listChats(
            @PageableDefault(size = 5, direction = Sort.Direction.ASC) Pageable page) {
        return ResponseEntity
                .ok(pagedResourcesAssembler.toModel(chatHistoryRepository.findAll(page), chatModelAssembler()));
    }

    private SimpleRepresentationModelAssembler<ChatHistory> chatModelAssembler() {
        return new SimpleRepresentationModelAssembler<ChatHistory>(){
            @Override
            public void addLinks(EntityModel<ChatHistory> resource) {

            }

            @Override
            public void addLinks(CollectionModel<EntityModel<ChatHistory>> resources) {

            }
        };
    }
}
