package com.dot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DotApplication {

    public static void main(String[] args) {
        // 14일 읽기전용, 미래 날짜 판정 등 날짜 계산이 KST 기준이어야 하므로
        // 서버(Docker 컨테이너, 기본 UTC)의 JVM 기본 타임존을 명시적으로 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(DotApplication.class, args);
    }
}
