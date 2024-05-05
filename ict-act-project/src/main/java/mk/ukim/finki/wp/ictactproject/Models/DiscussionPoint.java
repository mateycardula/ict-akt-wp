package mk.ukim.finki.wp.ictactproject.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@Entity
@AllArgsConstructor
public class DiscussionPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;

    private String discussion;

    @OneToMany
    private List<Member> votesYes;

    @OneToMany
    private List<Member> votesNo;

    @OneToMany
    private List<Member> abstained;

    private boolean confirmed;

    public DiscussionPoint() {
    }

    public DiscussionPoint(String topic, String discussion, List<Member> votesYes, List<Member> votesNo, List<Member> abstained, boolean confirmed) {
        this.topic = topic;
        this.discussion = discussion;
        this.votesYes = votesYes;
        this.votesNo = votesNo;
        this.abstained = abstained;
        this.confirmed = confirmed;
    }
}
