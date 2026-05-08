package io.auctionsystem.server.repogistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.auctionsystem.server.model.Art;

@Repository
public interface ArtRepository extends JpaRepository<Art, Long>{
    
}
