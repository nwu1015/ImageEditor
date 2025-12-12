package com.example.imageeditor.controller;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.CollageRepository;
import com.example.imageeditor.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final CollageRepository collageRepository;

    @GetMapping("/api/images/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        try {
            Path file = Paths.get("uploads").resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Неможливо прочитати файл");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/my-images")
    public String showMyImages(Model model, @AuthenticationPrincipal User user) {
        List<Image> images = imageService.findImagesByUser(user);
        model.addAttribute("images", images);

        List<Collage> collages = collageRepository.findByUserOrderByLastModifiedAtDesc(user);
        model.addAttribute("collages", collages);

        return "my-images";
    }

    @PostMapping("/images/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        try {
            imageService.storeImage(file, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Файл успішно завантажено!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/my-images";
    }

    @PostMapping("/images/update/{id}")
    public String updateImageTitle(@PathVariable("id") Long id,
                                   @RequestParam("title") String title,
                                   @AuthenticationPrincipal User user) {
        try {
            imageService.updateImageTitle(id, title, user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/my-images";
    }

    @PostMapping("/images/delete/{id}")
    public String deleteImage(@PathVariable("id") Long id,
                              @AuthenticationPrincipal User user) {
        try {
            imageService.deleteImage(id, user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/my-images";
    }
}