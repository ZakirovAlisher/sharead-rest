package com.example.site.repository;

import com.example.site.domain.Exchanges;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface ExchangeRepository extends JpaRepository<Exchanges, Long> {

    List<Exchanges> getExchangesByStatusOrderByDateDesc(String status);
}