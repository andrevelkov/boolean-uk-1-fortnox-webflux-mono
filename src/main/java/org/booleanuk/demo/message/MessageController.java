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

    @PostMapping
    public Mono<ResponseEntity<Void>> sendMessage(
            @RequestBody Message message,
            @RequestParam(required = false) Integer target_user) {

        return Mono.fromCallable(() -> {
            if (target_user != null) {
                Optional<User> target = userRepo.findById(target_user);

                if (target.isEmpty())
                    return ResponseEntity.notFound().<Void>build();

                message.setTargetUser(target.get());
                messageRepo.save(message);
                return ResponseEntity.ok().<Void>build();
            }

            // Broadcast message create a copy per user, for managing the deletion after response
            List<User> allUsers = userRepo.findAll();
            for (User user: allUsers) {
                Message copy = new Message();
                copy.setText(message.getText());
                copy.setDate(message.getDate());
                copy.setTargetUser(user);
                messageRepo.save(copy);
            }

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
                    List<Message> messages = messageRepo.findMessagesForUserOrBroadcast(user);

                    if (!messages.isEmpty()) {
                        messageRepo.deleteAll(messages);
                        return Mono.just(ResponseEntity.ok(messages));
                    }

                    return pollForNextMessage(user)
                            .timeout(Duration.ofMinutes(2))
                            .map(ResponseEntity::ok);
                });
    }

    private Mono<List<Message>> pollForNextMessage(User user) {

        return Mono.fromCallable(() -> messageRepo.findMessagesForUserOrBroadcast(user))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(messages -> {

                    if (!messages.isEmpty()) {
                        messageRepo.deleteAll(messages);
                        return Mono.just(messages);
                    }

                    return Mono.delay(Duration.ofSeconds(2))
                            .then(pollForNextMessage(user));
                });
    }

}
