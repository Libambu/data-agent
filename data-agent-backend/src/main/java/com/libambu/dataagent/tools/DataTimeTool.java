package com.libambu.dataagent.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class DataTimeTool {

    @Tool(description = "获取用户在指定时区的当前时间，格式为yyyy-MM-dd HH:mm:ss，用于回答需要实时时间的问题")
    public String getCurrentTime() {
        //获取用户的时区偏好设置
        var zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        var now = LocalDateTime.now(zoneId);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(now);
    }

    @Tool(description = "设置一个闹钟提醒")
    public void setAlarm(String time) {
        log.info("设置了一个闹钟提醒，时间是：{}", time);   
    }


    
}
