package com.example.admin.controller;

import com.example.site.domain.Authors;
import com.example.site.domain.Books;
import com.example.site.domain.Genres;
import com.example.site.domain.Users;
import com.example.site.service.AuthorService;
import com.example.site.service.BookService;
import com.example.site.service.GenreService;
import com.example.site.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminBookController {

    @Autowired
    UserService userService;

    @Autowired
    GenreService genreService;

    @Autowired
    AuthorService authorService;

    @Autowired
    BookService bookService;

    @Value("${file.book.uploadPath}")
    private String uploadPath;

    @Value("${file.book.viewPath}")
    private String viewPath;

    @GetMapping(value = "/book_details/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MODERATOR')")
    public String bookDetails(Model m, @PathVariable(name = "id") Long id){
        Books book = bookService.getBook(id);
        List<Genres> genres = genreService.getAllGenres();
        List<Authors> authors = authorService.getAllAuthors();

        m.addAttribute("genres", genres);
        m.addAttribute("authors", authors);
        m.addAttribute("book", book);

        m.addAttribute("currentUser", getUserData());

        return "edit_book";
    }

    @PostMapping(value = "/assign_genre")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MODERATOR')")
    public String assGenre(
            @RequestParam(name= "book_id") Long bookId,
            @RequestParam(name= "genre_id") Long genreId,
            @RequestParam(name="del",defaultValue = "0") int del

                        )
    {

        Genres genre = genreService.getGenre(genreId);
        if (genre != null) {
            Books book = bookService.getBook(bookId);
            if (book != null) {
                List<Genres> genres = book.getGenres();
                if(del == 1) {
                    genres.remove(genre);

                } else {
                    if (genres == null) {
                        genres = new ArrayList<>();
                    }
                    genres.add(genre);
                }
                bookService.saveBook(book);
                return "redirect:/book_details/" + bookId;
            }
        }


        return "redirect:/";
    }



    @PostMapping(value = "/addBook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MODERATOR')")
    public String addItem(@RequestParam(name="title") String name,
                          @RequestParam(name="author_id", defaultValue = "0") Long id,
                          @RequestParam(name="cover") MultipartFile file

                         )



    {
        Authors br = authorService.getAuthor(id);

        if(br!=null){
            if(file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png")) {

                try {
                    String picName = DigestUtils.sha1Hex("book_"+name+"_!Picture");
                    byte[] bytes = file.getBytes();
                    Path path = Paths.get(uploadPath + picName+".jpg");
                    Files.write(path, bytes);

                    bookService.addBook(new Books(null, name, picName, br, null));
                    return "redirect:/admin" ;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }


        return "redirect:/admin";
    }

    @PostMapping(value = "/editBook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MODERATOR')")
    public String saveTask(
            @RequestParam(name="id") Long id,
            @RequestParam(name="title") String name,
            @RequestParam(name="author_id") Long authorId,
            @RequestParam(name="del", defaultValue = "0") int del

                          )
    {

        if(del == 1){
            bookService.deleteBook(bookService.getBook(id));

            return "redirect:/admin";
        }
        else{
            Books i = bookService.getBook(id);
            i.setTitle(name);
            i.setAuthor(authorService.getAuthor(id));

            bookService.saveBook(i);

            return "redirect:/book_details/ "+ i.getId ();
        }}

    @PostMapping(value = "/editBookCover")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MODERATOR')")
    public String saveTask(
            @RequestParam(name="id") Long id,
            @RequestParam(name="cover") MultipartFile file

                          )
    {

            Books i = bookService.getBook(id);

            if(file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png")) {

                try {
                    String picName = DigestUtils.sha1Hex("book_"+i.getTitle()+"_!Picture");
                    byte[] bytes = file.getBytes();
                    Path path = Paths.get(uploadPath + picName+".jpg");
                    Files.write(path, bytes);

                    i.setCover(picName);
                    return "redirect:/book_details/ "+ i.getId ();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            bookService.saveBook(i);

            return "redirect:/book_details/ "+ i.getId ();
        }

    @GetMapping(value = "/viewbook/{url}", produces={MediaType.IMAGE_JPEG_VALUE})

    public @ResponseBody
    byte[] viewProfilePhoto(@PathVariable(name = "url") String url) throws IOException {
        String pictureURL = "";
        if(url!=null){
            pictureURL=viewPath+url+".jpg";
        }
        InputStream in;
        try {
            ClassPathResource resource = new ClassPathResource(pictureURL);
            in = resource.getInputStream();
        }
        catch (Exception e){
            ClassPathResource resource = new ClassPathResource(viewPath);
            in = resource.getInputStream();
            e.printStackTrace();
        }

        return org.apache.commons.io.IOUtils.toByteArray(in);
    }

    private Users getUserData(){
        Authentication authontication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authontication instanceof AnonymousAuthenticationToken)){
            User secUser = (User)authontication.getPrincipal();
            Users myUser = userService.getUserByEmail(secUser.getUsername());
            return myUser;
        }
        return null;
    }


}
