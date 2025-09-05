package org.booleanuk.demo.message;

import org.booleanuk.demo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByTargetUser(User user);
}
