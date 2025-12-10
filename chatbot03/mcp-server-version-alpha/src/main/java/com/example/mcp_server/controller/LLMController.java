package com.example.mcp_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.mcp_server.service.LLMService;

@RestController
@RequestMapping("/api")
public class LLMController {
    private final LLMService llmService;

    public LLMController(LLMService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/query")
    public String handleQuery(@RequestBody String naturalLanguageQuery) {
        return llmService.processQuery(naturalLanguageQuery);
    }

   @GetMapping("/ask-gemini")
    public String askGemini(@RequestParam("question") String question) {
        return llmService.askGemini(question);
    }
}
