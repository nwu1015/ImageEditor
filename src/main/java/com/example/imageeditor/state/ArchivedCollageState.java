package com.example.imageeditor.state;

import com.example.imageeditor.domain.Collage;

public class ArchivedCollageState implements CollageState {
    @Override
    public String getStatusName() { return "ARCHIVED"; }

    @Override
    public void checkCanEdit(Collage context) {
        throw new IllegalStateException("Cannot edit an ARCHIVED collage. Restore it first.");
    }

    @Override
    public void publish(Collage context) {
        throw new IllegalStateException("Cannot publish an ARCHIVED collage. Restore it first.");
    }

    @Override
    public void archive(Collage context) {
    }

    @Override
    public void restore(Collage context) {
        context.changeState(new DraftCollageState());
    }
}
