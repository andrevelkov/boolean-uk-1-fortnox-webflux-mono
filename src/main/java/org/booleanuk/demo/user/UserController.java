package org.booleanuk.demo.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        return Mono.fromCallable(() -> userRepo.save(user));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Void>> getUser(@PathVariable int id) {
        return Mono.fromCallable(() -> {
            Optional<User> user = userRepo.findById(id);
            if (user.isPresent()) {
                return ResponseEntity.ok().<Void>build();
            }
            return ResponseEntity.notFound().<Void>build();
        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Void>> updateUser(@PathVariable int id, @RequestBody User body) {
        return Mono.fromCallable(() ->  {
            Optional<User> user = userRepo.findById(id);

            if (user.isPresent()) {
                user.get().setUsername(body.getUsername());
                return ResponseEntity.ok().<Void>build();
            }
            return ResponseEntity.notFound().<Void>build();
        });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable int id) {
        return Mono.fromCallable(() -> {
            Optional<User> user = userRepo.findById(id);

            if (user.isPresent()) {
                userRepo.deleteById(id);
                return ResponseEntity.ok().<Void>build();
            }
            return ResponseEntity.notFound().<Void>build();
        });
    }




}
