package com.example.imageeditor.domain;

import com.example.imageeditor.state.ArchivedCollageState;
import com.example.imageeditor.state.CollageState;
import com.example.imageeditor.state.DraftCollageState;
import com.example.imageeditor.state.PublishedCollageState;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collages")
@Data
@NoArgsConstructor
public class Collage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private int canvasWidth;
    private int canvasHeight;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastModifiedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "collage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("zIndex ASC")
    private List<LayerComponent> layers = new ArrayList<>();

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(name = "preview_filename")
    private String previewFileName;

    @Transient
    private CollageState currentState;

    @PostLoad
    public void initState() {
        switch (this.status) {
            case "PUBLISHED":
                this.currentState = new PublishedCollageState();
                break;
            case "ARCHIVED":
                this.currentState = new ArchivedCollageState();
                break;
            default:
                this.currentState = new DraftCollageState();
                break;
        }
    }

    public void changeState(CollageState newState) {
        this.currentState = newState;
        this.status = newState.getStatusName();
    }

    public CollageState getCurrentState() {
        if (currentState == null) {
            initState();
        }
        return currentState;
    }
}
