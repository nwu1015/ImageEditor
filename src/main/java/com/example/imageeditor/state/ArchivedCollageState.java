package com.example.imageeditor.state;

import com.example.imageeditor.domain.Collage;

public class ArchivedCollageState implements CollageState {
    @Override
    public String getStatusName() { return "ARCHIVED"; }

    @Override
    public void checkCanEdit(Collage context) {
        throw new IllegalStateException("Неможливо змінювати архівований колаж");
    }

    @Override
    public void publish(Collage context) {
        throw new IllegalStateException("Неможливо опублікувати архівований колаж");
    }

    @Override
    public void archive(Collage context) {
    }

    @Override
    public void restore(Collage context) {
        context.changeState(new DraftCollageState());
    }
}
