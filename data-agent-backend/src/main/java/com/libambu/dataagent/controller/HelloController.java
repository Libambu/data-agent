package com.libambu.dataagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对应原 data-agent-backend 中 Kotlin 版的 HelloController
 */
@RestController
public class HelloController {

    public record Message(String message) {
    }

    @GetMapping("/hello")
    public Message hello() {
        return new Message("QiFan");
    }
}
