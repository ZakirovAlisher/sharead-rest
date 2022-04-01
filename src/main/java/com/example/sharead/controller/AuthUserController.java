package com.example.sharead.controller;

import com.example.sharead.domain.Roles;
import com.example.sharead.domain.Users;
import com.example.sharead.service.UserService;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.*;
import java.util.List;

import static java.util.Collections.reverse;

@Controller

public class AuthUserController {


    private  SecretKeySpec secretKey;
    private  byte[] key;

    public  void setKey(String myKey)
    {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public  String encrypt(String strToEncrypt, String secret)
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public  String decrypt(String strToDecrypt, String secret)
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

    @Autowired
    private UserService userService;


    @Value("${file.avatar.viewPath}")
    private String viewPath;

    @Value("${file.avatar.uploadPath}")
    private String uploadPath;

    @Value("${file.avatar.defaultPicture}")
    private String defaultPicture;




    @GetMapping(value = "/login")
    public String login(Model m){
        m.addAttribute("currentUser", getUserData());
        return "login";
    }

    @GetMapping(value = "/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Model m){
        m.addAttribute("currentUser", getUserData());
        return "profile";
    }
    @GetMapping(value = "/register")

    public String reg(Model m){
        m.addAttribute("currentUser", getUserData());
        return "register";
    }

    @PostMapping(value = "/reg")

    public String regis(@RequestParam(name="name") String name,
                        @RequestParam(name="pass") String pass,
                        @RequestParam(name="pass2") String pass2,
                        @RequestParam(name="email") String email
            ,
                        RedirectAttributes redirAttrs
                       )



    {


        if(pass.equals(pass2)){
//            ArrayList<Roles> r = new ArrayList<Roles>();
//            r.add(userService.getRole(1L));
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            userService.addUser(new Users(null,email,passwordEncoder.encode(pass), name, null,null));


            redirAttrs.addFlashAttribute("success", "Successfully registred");

            return "redirect:/login";}
        else
        {redirAttrs.addFlashAttribute("error", "Registration error");
            return "redirect:/register?error";}
    }


    @PostMapping(value = "/edprofile")

    public String saveUser2(
            @RequestParam(name="id") Long id,
            @RequestParam(name="name") String name,
            RedirectAttributes redirAttrs

                           )
    {

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        Users i = userService.getUser(id);
        i.setFullName(name);

        userService.saveUser(i);
        redirAttrs.addFlashAttribute("success1", "Profile successfully updated.");
        return "redirect:/profile" ;
    }

    @PostMapping(value = "/edpass")

    public String saveUser2(
            @RequestParam(name="id") Long id,
            @RequestParam(name="old") String old,

            @RequestParam(name="new") String new1,
            @RequestParam(name="new2") String new2,RedirectAttributes redirAttrs


                           )
    {


        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(old, getUserData().getPassword())){
            Users i = userService.getUser(id);
            if(new1.equals(new2)){
                i.setPassword(passwordEncoder.encode(new1));
                userService.saveUser(i);}
            else {

                redirAttrs.addFlashAttribute("errorP", "Confirm password doesnt match.");
                return "redirect:/profile?differentpasswords" ;}
        }
        else  {


            redirAttrs.addFlashAttribute("errorP2", " Old password doesnt match.");
            return "redirect:/profile?olddoesntmatch" ;}






        redirAttrs.addFlashAttribute("successP", "Password successfully updated.");
        return "redirect:/profile" ;
    }


    @PostMapping(value = "/uploadavatar")
    @PreAuthorize("isAuthenticated()")
    public String avatar(
            @RequestParam(name="user_ava") MultipartFile file
            ,RedirectAttributes redirAttrs

                        )
    {
        if(file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png")) {

            try {
                Users currentUser = getUserData();

                String picName = DigestUtils.sha1Hex("avatar_"+currentUser.getId()+"_!Picture");
                byte[] bytes = file.getBytes();
                Path path = Paths.get(uploadPath + picName+".jpg");
                Files.write(path, bytes);
                currentUser.setUserAvatar(picName);
                userService.saveUser(currentUser);
                redirAttrs.addFlashAttribute("successA", "Successfully updated avatar.");
                return "redirect:/profile?success" ;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        redirAttrs.addFlashAttribute("errorA", "Error download avatar.");
        return "redirect:/profile" ;
    }



    @GetMapping(value = "/viewphoto/{url}", produces={MediaType.IMAGE_JPEG_VALUE})

    public @ResponseBody byte[] viewProfilePhoto(@PathVariable(name = "url") String url) throws IOException {
        String pictureURL = viewPath+defaultPicture;
        if(url!=null){
            pictureURL=viewPath+url+".jpg";
        }
        InputStream in;
        try {
            ClassPathResource resource = new ClassPathResource(pictureURL);
            in = resource.getInputStream();
        }
        catch (Exception e){
            ClassPathResource resource = new ClassPathResource(viewPath+defaultPicture);
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





