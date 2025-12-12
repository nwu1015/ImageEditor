package com.example.imageeditor.state;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.service.CollageService;
import org.springframework.web.multipart.MultipartFile;

public class PublishedCollageState implements CollageState {
    @Override
    public String getStatusName() { return "PUBLISHED"; }

    @Override
    public void checkCanEdit(Collage context) {
        throw new IllegalStateException(
                "Неможливо змінювати файл, який вже опублікований");
    }

    @Override
    public void publish(Collage context) {
    }

    @Override
    public void archive(Collage context) {
        context.changeState(new ArchivedCollageState());
    }

    @Override
    public void restore(Collage context) {
        throw new IllegalStateException(
                "Неможливо відновити файл з чорновика");
    }
}
