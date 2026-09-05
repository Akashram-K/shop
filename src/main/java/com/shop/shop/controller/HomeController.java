package com.shop.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping(value = {
            "/",
            "/login",
            "/register",
            "/cart",
            "/orders",
            "/profile",
            "/admin",
            "/products"
    })
    public String home() {
        return "forward:/index.html";
    }
}
