package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Tag;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    Long countByName(String name);
}