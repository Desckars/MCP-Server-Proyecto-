package com.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador para servir las páginas HTML
 */
@Controller
@RequestMapping("/")
public class ViewController {

    /**
     * Página principal del ChatBot
     */
    @GetMapping({"/", "/index", "/index.html"})
    public String index() {
        return "index";
    }

    /**
     * Página de console (interfaz de consola web)
     */
    @GetMapping({"/console", "/console.html"})
    public String console() {
        return "console";
    }
}
