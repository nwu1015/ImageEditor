package com.example.imageeditor.domain;

public record ImageLayerMemento(
        Long layerId,
        int positionX,
        int positionY,
        int width,
        int height,
        double rotationAngle,
        Integer cropX,
        Integer cropY,
        Integer cropWidth,
        Integer cropHeight
) {

    public ImageLayerMemento(ImageLayer layer) {
        this(
                layer.getId(),
                layer.getPositionX(),
                layer.getPositionY(),
                layer.getWidth(),
                layer.getHeight(),
                layer.getRotationAngle(),
                layer.getCropX(),
                layer.getCropY(),
                layer.getCropWidth(),
                layer.getCropHeight()
        );
    }
}