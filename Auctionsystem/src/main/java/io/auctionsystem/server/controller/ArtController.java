package io.auctionsystem.server.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.service.ArtService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/art")
public class ArtController {

    private final ArtService artService;
    
    @GetMapping
    public ResponseEntity<List<Art>> getAllArts() {
        return artService.getAllArts();
    }
}
