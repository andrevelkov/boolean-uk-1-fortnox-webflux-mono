package org.booleanuk.demo.message;

import org.booleanuk.demo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByTargetUser(User user);

    @Query("SELECT m FROM Message m " +
            "WHERE m.targetUser = :user OR m.targetUser IS NULL")
    List<Message> findMessagesForUserOrBroadcast(@Param("user") User user);

}
