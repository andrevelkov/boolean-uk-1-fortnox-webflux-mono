package org.booleanuk.demo.message;

import org.booleanuk.demo.user.User;
import org.booleanuk.demo.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageRepository messageRepo;
    private final UserRepository userRepo;

    public MessageController(MessageRepository messageRepo, UserRepository userRepo) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
    }

    @PostMapping("/{target_user}")
    public Mono<ResponseEntity<Void>> sendMessage(
            @RequestBody Message message,
            @PathVariable(required = false) Integer target_user) {

        return Mono.fromCallable(() -> {
            Optional<User> target = userRepo.findById(target_user);

            if (target.isEmpty()) {
                return ResponseEntity.notFound().<Void>build();
            }

            message.setTargetUser(target.get());
            messageRepo.save((message));

            return ResponseEntity.ok().<Void>build();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/listen/{userId}")
    public Mono<ResponseEntity<List<Message>>> listenToMessages(@PathVariable int userId) {

        return Mono.fromCallable(() -> userRepo.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userOpt -> {

                    if (userOpt.isEmpty())
                        return Mono.just(ResponseEntity.notFound().<List<Message>>build());

                    User user = userOpt.get();
                    List<Message> messages = messageRepo.findByTargetUser(user);

                    if (!messages.isEmpty()) {
                        messageRepo.deleteAll();
                        return Mono.just(ResponseEntity.ok(messages));
                    }

                    return pollForNextMessage(user)
                            .timeout(Duration.ofMinutes(2))
                            .map(ResponseEntity::ok);
                });
    }

    private Mono<List<Message>> pollForNextMessage(User user) {

        return Mono.fromCallable(() -> messageRepo.findByTargetUser(user))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(messages -> {

                    if (!messages.isEmpty())
                        return Mono.just(messages);

                    return Mono.delay(Duration.ofSeconds(2))
                            .then(pollForNextMessage(user));
                });
    }

}
