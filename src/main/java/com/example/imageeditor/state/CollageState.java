package com.example.imageeditor.state;

import com.example.imageeditor.domain.Collage;

public interface CollageState {
    String getStatusName();

    void checkCanEdit(Collage context);
    void publish(Collage context);
    void archive(Collage context);
    void restore(Collage context);
}
