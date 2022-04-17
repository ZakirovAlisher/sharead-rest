package com.example.site.repository;

import com.example.site.domain.Books;
import com.example.site.domain.UserBooks;
import com.example.site.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface UserBookRepository extends JpaRepository<UserBooks, Long> {

    List<UserBooks> findUserBooksByUser(Users user);

}