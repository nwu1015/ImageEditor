package com.example.imageeditor.state;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.service.CollageService;
import org.springframework.web.multipart.MultipartFile;

public class DraftCollageState implements CollageState {
    @Override
    public String getStatusName() { return "DRAFT"; }

    @Override
    public void checkCanEdit(Collage context) {
        // Дозволено.
    }

    @Override
    public void publish(Collage context) {
        context.changeState(new PublishedCollageState());
    }

    @Override
    public void archive(Collage context) {
        context.changeState(new ArchivedCollageState());
    }

    @Override
    public void restore(Collage context) {
        throw new IllegalStateException("Cannot restore from Draft state.");
    }
}
