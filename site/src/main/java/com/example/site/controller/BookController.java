package com.example.site.controller;

import com.example.site.domain.Books;
import com.example.site.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
class BookController {

  @Autowired
  private BookRepository bookRepository;



  // Aggregate root
  // tag::get-aggregate-root[]
  @GetMapping("/books")
  List<Books> all() {
    return bookRepository.findAll();
  }
  // end::get-aggregate-root[]

  @PostMapping("/books")
  Books newEmployee(@RequestBody Books newEmployee) {
    return bookRepository.save(newEmployee);
  }

  // Single item
  
  @GetMapping("/books/{id}")
  Optional<Books> one(@PathVariable Long id) {
    
    return bookRepository.findById(id);
  }


  @DeleteMapping("/books/{id}")
  void deleteEmployee(@PathVariable Long id) {
    bookRepository.deleteById(id);
  }
}