package com.example.imageeditor.domain;

import com.example.imageeditor.service.CollageService;
import com.example.imageeditor.service.ImageService;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("COMPOSITE")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LayerGroup extends LayerComponent {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "parent_group_id")
    @OrderBy("zIndex ASC")
    private List<LayerComponent> children = new ArrayList<>();

    @Override
    public void render(Graphics2D g2d, ImageService imageService) throws IOException {
        for (LayerComponent child : children) {
            child.render(g2d, imageService);
        }
    }

    @Override
    public void applyUpdate(CollageService.LayerUpdateDTO dto) {
        for (LayerComponent child : children) {
            child.applyUpdate(dto);
        }
    }

    @Override
    public LayerComponent clone() {
        LayerGroup newGroup = (LayerGroup) super.clone();
        newGroup.setId(null);

        newGroup.setChildren(new ArrayList<>());
        for (LayerComponent child : this.children) {
            LayerComponent clonedChild = child.clone();
            newGroup.add(clonedChild);
        }
        return newGroup;
    }

    @Override
    public ImageLayerMemento createMemento() {
        throw new UnsupportedOperationException("Memento не підтримується для цієї групи");
    }

    @Override
    public void restoreFromMemento(ImageLayerMemento memento) {
        throw new UnsupportedOperationException("Memento не підтримується для цієї групи");
    }

    public void add(LayerComponent component) {
        children.add(component);
    }
}