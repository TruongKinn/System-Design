package com.dataflow.export.service;

import com.dataflow.export.dto.ExportProgressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class RedisProgressService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ConcurrentMap<String, ExportProgressDto> localFallbackMap = new ConcurrentHashMap<>();

    public void updateProgress(String jobId, String fileName, String status, long total, long processed, long success, long error) {
        double percent = total > 0 ? (double) processed / total * 100.0 : 0.0;

        ExportProgressDto dto = ExportProgressDto.builder()
                .jobId(jobId)
                .fileName(fileName)
                .status(status)
                .totalRecords(total)
                .processedRecords(processed)
                .successRecords(success)
                .errorRecords(error)
                .percentComplete(Math.round(percent * 100.0) / 100.0)
                .build();

        if (redisTemplate != null) {
            try {
                String key = "export:progress:" + jobId;
                redisTemplate.opsForHash().put(key, "status", status);
                redisTemplate.opsForHash().put(key, "total", String.valueOf(total));
                redisTemplate.opsForHash().put(key, "processed", String.valueOf(processed));
                redisTemplate.opsForHash().put(key, "success", String.valueOf(success));
                redisTemplate.opsForHash().put(key, "error", String.valueOf(error));
                redisTemplate.opsForHash().put(key, "percent", String.valueOf(percent));
            } catch (Exception e) {
                log.warn("Redis unreachable, updating local fallback progress map for jobId: {}", jobId);
            }
        }
        localFallbackMap.put(jobId, dto);
    }

    public ExportProgressDto getProgress(String jobId) {
        if (redisTemplate != null) {
            try {
                String key = "export:progress:" + jobId;
                Object statusObj = redisTemplate.opsForHash().get(key, "status");
                if (statusObj != null) {
                    long total = parseLong(redisTemplate.opsForHash().get(key, "total"));
                    long processed = parseLong(redisTemplate.opsForHash().get(key, "processed"));
                    long success = parseLong(redisTemplate.opsForHash().get(key, "success"));
                    long error = parseLong(redisTemplate.opsForHash().get(key, "error"));
                    double percent = total > 0 ? (double) processed / total * 100.0 : 0.0;

                    return ExportProgressDto.builder()
                            .jobId(jobId)
                            .status(statusObj.toString())
                            .totalRecords(total)
                            .processedRecords(processed)
                            .successRecords(success)
                            .errorRecords(error)
                            .percentComplete(Math.round(percent * 100.0) / 100.0)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Redis error on getProgress, reading local fallback for jobId: {}", jobId);
            }
        }
        return localFallbackMap.getOrDefault(jobId, ExportProgressDto.builder().jobId(jobId).status("UNKNOWN").build());
    }

    private long parseLong(Object obj) {
        return obj != null ? Long.parseLong(obj.toString()) : 0L;
    }
}
