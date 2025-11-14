package com.example.imageeditor.domain;

import com.example.imageeditor.service.Prototype;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList; // Додано
import java.util.List; // Додано

@Entity
@Table(name = "image_layers")
@Data
@NoArgsConstructor
public class ImageLayer implements Prototype, Cloneable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Collage collage;

    // --- ЗМІНА 1 ---
    // Робимо "image" необов'язковим, оскільки "Група" не має одного зображення.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id") // 'nullable = false' було видалено
    private Image image;

    // Властивості трансформації (для Листка ТА Групи)
    private int positionX;
    private int positionY;
    private double rotationAngle = 0.0;
    private int zIndex;

    // Властивості (ТІЛЬКИ для Листка)
    private int width;  // Ширина шару
    private int height; // Висота шару
    private Integer cropX;
    private Integer cropY;
    private Integer cropWidth;
    private Integer cropHeight;
    @Column(columnDefinition = "TEXT")
    private String effectsJson;

    // --- ЗМІНА 2: Додаємо поля для Composite ---

    @Column(name = "is_group", nullable = false, columnDefinition = "boolean default false")
    private boolean isGroup = false; // За замовчуванням це "Листок" (Leaf)

    // Зв'язок "один-до-багатьох" з самим собою.
    // Це список "нащадків", якщо isGroup = true.
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_layer_id")
    @OrderBy("zIndex ASC")
    private List<ImageLayer> children = new ArrayList<>();

    // --- ЗМІНА 3: Додаємо методи для керування групою ---

    public void add(ImageLayer child) {
        if (!this.isGroup) {
            throw new UnsupportedOperationException("Неможливо додати нащадка до 'Листка' (Leaf).");
        }
        children.add(child);
    }

    public void remove(ImageLayer child) {
        if (!this.isGroup) {
            throw new UnsupportedOperationException("Неможливо видалити нащадка з 'Листка' (Leaf).");
        }
        children.remove(child);
    }

    // --- ЗМІНА 4: Оновлюємо CLONE для глибокого копіювання ---

    @Override
    public ImageLayer clone() {
        try {
            // 1. Створюємо поверхневу копію
            ImageLayer newLayer = (ImageLayer) super.clone();

            // 2. Встановлюємо базові властивості нового об'єкта
            newLayer.setId(null); // Нова сутність
            newLayer.setCollage(this.getCollage());
            newLayer.setGroup(this.isGroup);

            // 3. ✨ ВИПРАВЛЕННЯ: Ініціалізуємо НОВИЙ список "нащадків"
            // Це розриває "shared reference" з оригінальним об'єктом.
            // Це потрібно робити ЗАВЖДИ, і для "Листків", і для "Груп".
            newLayer.setChildren(new ArrayList<>());

            // 4. Копіюємо загальні властивості
            newLayer.setPositionX(this.positionX);
            newLayer.setPositionY(this.positionY);
            newLayer.setRotationAngle(this.rotationAngle);
            // zIndex буде встановлено в сервісі

            if (this.isGroup) {
                // 5. Якщо це група, рекурсивно клонуємо "нащадків"
                // і додаємо їх у НОВИЙ список
                for (ImageLayer child : this.children) {
                    ImageLayer clonedChild = child.clone(); // Рекурсивний виклик
                    newLayer.add(clonedChild);
                }
            } else {
                // 6. Якщо це "Листок", копіюємо його унікальні властивості
                newLayer.setImage(this.getImage());
                newLayer.setWidth(this.width);
                newLayer.setHeight(this.height);
                newLayer.setCropX(this.cropX);
                newLayer.setCropY(this.cropY);
                newLayer.setCropWidth(this.cropWidth);
                newLayer.setCropHeight(this.cropHeight);
                newLayer.setEffectsJson(this.effectsJson);
            }

            return newLayer;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Неможливо клонувати шар.", e);
        }
    }

    // --- (Ваші методи Memento залишаються) ---
    // (Примітка: Memento тепер зберігатиме лише стан "Листка".
    // Memento для груп - це набагато складніша логіка)
    public ImageLayerMemento createMemento() {
        if (this.isGroup) {
            throw new UnsupportedOperationException("Memento не підтримується для груп.");
        }
        return new ImageLayerMemento(this);
    }

    public void restoreFromMemento(ImageLayerMemento memento) {
        this.setPositionX(memento.positionX());
        this.setPositionY(memento.positionY());
        this.setWidth(memento.width());
        this.setHeight(memento.height());
        this.setRotationAngle(memento.rotationAngle());
        this.setCropX(memento.cropX());
        this.setCropY(memento.cropY());
        this.setCropWidth(memento.cropWidth());
        this.setCropHeight(memento.cropHeight());
    }
}