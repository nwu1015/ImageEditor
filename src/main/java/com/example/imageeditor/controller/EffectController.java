package com.example.imageeditor.controller;

import com.example.imageeditor.entity.Effect;
import com.example.imageeditor.service.EffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/effects")
@RequiredArgsConstructor
public class EffectController {

    private final EffectService effectService;

    @PostMapping("/add/{imageId}")
    public ResponseEntity<Effect> addEffect(@RequestBody Effect effect,
                                            @PathVariable int imageId) {
        Effect savedEffect = effectService.addEffectToImage(effect, imageId);
        return ResponseEntity.ok(savedEffect);
    }

    @GetMapping("/image/{imageId}")
    public ResponseEntity<List<Effect>> getImageEffects(@PathVariable int imageId) {
        return ResponseEntity.ok(effectService.getEffectsByImage(imageId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Effect> getEffectById(@PathVariable int id) {
        return effectService.getEffectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEffect(@PathVariable int id) {
        effectService.deleteEffect(id);
        return ResponseEntity.noContent().build();
    }
}
