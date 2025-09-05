package org.booleanuk.demo.message;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.booleanuk.demo.user.User;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "messages")
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String text;
    private String date;

    @ManyToOne
    @JoinColumn(name = "target_user") // FK column
    @JsonBackReference
    private User targetUser;
}
