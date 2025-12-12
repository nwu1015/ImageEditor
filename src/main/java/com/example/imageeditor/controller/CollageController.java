package com.example.imageeditor.controller;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.ImageRepository;
import com.example.imageeditor.service.CollageService;
import com.example.imageeditor.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/collages")
@RequiredArgsConstructor
public class CollageController {

    private final CollageService collageService;

    private final ImageRepository imageRepository;

    private final ImageService imageService;

    @GetMapping("/from-image/{imageId}")
    public String editCollageFromImage(@PathVariable Long imageId, @AuthenticationPrincipal User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Зображення не знайдено!"));

        if (!image.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Відмова доступу");
        }

        ImageLayer layer = collageService.findOrCreateLayerForImage(image, user);
        Long collageId = layer.getCollage().getId();

        return "redirect:/collages/" + collageId;
    }

    @GetMapping("/{collageId}")
    public String showEditorPage(@PathVariable Long collageId, Model model) {
        model.addAttribute("collage", collageService.findCollageById(collageId));
        return "editor";
    }

    @PostMapping("/{collageId}/layers/add")
    public String handleImageUpload(@PathVariable Long collageId,
                                    @RequestParam("imageFile") MultipartFile file,
                                    @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        try {
            collageService.addImageToCollage(collageId, file, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Зображення додано!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Помилка завантаження файлу.");
            e.printStackTrace();
        }
        return "redirect:/collages/" + collageId;
    }

    @PostMapping("/{collageId}/layers/{layerId}/action")
    public String handleLayerAction(@PathVariable Long collageId,
                                    @PathVariable Long layerId,
                                    @RequestParam String action) {
        collageService.updateLayerAction(layerId, action);
        return "redirect:/collages/" + collageId;
    }

    @GetMapping("/layers/{layerId}/transformed")
    @ResponseBody
    public ResponseEntity<Resource> getTransformedLayer(@PathVariable Long layerId) {
        try {
            Resource resource = imageService.getTransformedLayerAsResource(layerId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{collageId}/render")
    public String renderCollage(@PathVariable Long collageId, @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        try {
            Image finalImage = collageService.renderAndSaveCollage(collageId, user);
            redirectAttributes.addFlashAttribute("finalImageId", finalImage.getId());
            return "redirect:/my-images";
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Не вдалося зберегти колаж.");
            return "redirect:/collages/" + collageId;
        }
    }

    @PostMapping("/{collageId}/layers/{layerId}/update")
    public String updateLayerDetails(@PathVariable Long collageId,
                                     @PathVariable Long layerId,
                                     @ModelAttribute CollageService.LayerUpdateDTO dto) {
        collageService.updateImageLayer(layerId, dto);
        return "redirect:/collages/" + collageId;
    }

    @PostMapping("/{collageId}/layers/{layerId}/clone")
    public String cloneLayer(@PathVariable Long collageId,
                                     @PathVariable Long layerId,
                                     @ModelAttribute CollageService.LayerUpdateDTO dto) {
        collageService.duplicateLayer(layerId);
        collageService.updateImageLayer(layerId, dto);
        return "redirect:/collages/" + collageId;
    }

    @PostMapping("/{collageId}/layers/{layerId}/undo")
    public String undoAction(@PathVariable Long collageId,
                             @PathVariable Long layerId,
                             @ModelAttribute CollageService.LayerUpdateDTO dto){
        collageService.undo(collageId);
        return "redirect:/collages/" + collageId;
    }

    @PostMapping("/{collageId}/layers/{layerId}/redo")
    public String redoAction(@PathVariable Long collageId,
                             @PathVariable Long layerId,
                             @ModelAttribute CollageService.LayerUpdateDTO dto){
        collageService.redo(collageId);
        return "redirect:/collages/" + collageId;
    }

    @PostMapping("/{collageId}/publish")
    public String publishCollage(@PathVariable Long collageId, RedirectAttributes redirectAttributes) {
        try {
            collageService.publishCollage(collageId);
            redirectAttributes.addFlashAttribute("successMessage", "Колаж успішно опубліковано!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Помилка при публікації колажу.");
            e.printStackTrace();
        }
        return "redirect:/my-images";
    }

    @PostMapping("/{collageId}/archive")
    public String archiveCollage(@PathVariable Long collageId, RedirectAttributes redirectAttributes) {
        try {
            collageService.archiveCollage(collageId);
            redirectAttributes.addFlashAttribute("successMessage", "Колаж переміщено в архів.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Помилка при архівуванні.");
            e.printStackTrace();
        }
        return "redirect:/my-images";
    }

    @PostMapping("/{collageId}/restore")
    public String restoreCollage(@PathVariable Long collageId, RedirectAttributes redirectAttributes) {
        try {
            collageService.restoreCollage(collageId);
            redirectAttributes.addFlashAttribute("successMessage", "Колаж відновлено до чернетки. Можна редагувати.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Помилка при відновленні.");
            e.printStackTrace();
        }
        return "redirect:/my-images";
    }

    @GetMapping("/{collageId}/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadCollage(@PathVariable Long collageId,
                                                    @RequestParam("format") String format) {
        try {
            Resource fileResource = collageService.generateCollageResource(collageId, format);
            MediaType mediaType = switch (format.toLowerCase()) {
                case "png" -> MediaType.IMAGE_PNG;
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "gif" -> MediaType.IMAGE_GIF;
                case "tiff", "tif" -> MediaType.parseMediaType("image/tiff");
                case "bmp" -> MediaType.parseMediaType("image/bmp");
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };

            String filename = fileResource.getFilename();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}