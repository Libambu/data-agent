package com.libambu.dataagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例接口：返回一个打招呼的消息。
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
