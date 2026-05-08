package io.auctionsystem.server.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.pattern.ItemFactory;
import io.auctionsystem.server.repogistory.ArtRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtService {
    
    private final ItemFactory itemFactory;
    private final ArtRepository artRepository;

    public ResponseEntity<List<Art>> getAllArts() {
        List<Art> arts = artRepository.findAll().stream()
                .map(item -> (Art) itemFactory.createItem(item)).collect(Collectors.toList());
        if(arts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(arts);
    }
 }
