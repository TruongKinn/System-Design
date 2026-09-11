package com.dataflow.importsvc.service;

import com.dataflow.importsvc.dto.ImportProgressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class RedisImportProgressService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ConcurrentMap<String, ImportProgressDto> localFallbackMap = new ConcurrentHashMap<>();

    public void updateProgress(String jobId, String fileName, String status, long total, long imported, long errors) {
        long processed = imported + errors;
        double percent = total > 0 ? (double) processed / total * 100.0 : 0.0;

        ImportProgressDto dto = ImportProgressDto.builder()
                .jobId(jobId)
                .fileName(fileName)
                .status(status)
                .totalRows(total)
                .importedRows(imported)
                .errorRows(errors)
                .percentComplete(Math.round(percent * 100.0) / 100.0)
                .build();

        if (redisTemplate != null) {
            try {
                String key = "import:progress:" + jobId;
                redisTemplate.opsForHash().put(key, "status", status);
                redisTemplate.opsForHash().put(key, "total", String.valueOf(total));
                redisTemplate.opsForHash().put(key, "imported", String.valueOf(imported));
                redisTemplate.opsForHash().put(key, "errors", String.valueOf(errors));
                redisTemplate.opsForHash().put(key, "percent", String.valueOf(percent));
            } catch (Exception e) {
                log.warn("Redis unreachable, updating local fallback progress map for import jobId: {}", jobId);
            }
        }
        localFallbackMap.put(jobId, dto);
    }

    public ImportProgressDto getProgress(String jobId) {
        if (redisTemplate != null) {
            try {
                String key = "import:progress:" + jobId;
                Object statusObj = redisTemplate.opsForHash().get(key, "status");
                if (statusObj != null) {
                    long total = parseLong(redisTemplate.opsForHash().get(key, "total"));
                    long imported = parseLong(redisTemplate.opsForHash().get(key, "imported"));
                    long errors = parseLong(redisTemplate.opsForHash().get(key, "errors"));
                    double percent = total > 0 ? (double) (imported + errors) / total * 100.0 : 0.0;

                    return ImportProgressDto.builder()
                            .jobId(jobId)
                            .status(statusObj.toString())
                            .totalRows(total)
                            .importedRows(imported)
                            .errorRows(errors)
                            .percentComplete(Math.round(percent * 100.0) / 100.0)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Redis error on getProgress, reading local fallback for import jobId: {}", jobId);
            }
        }
        return localFallbackMap.getOrDefault(jobId, ImportProgressDto.builder().jobId(jobId).status("UNKNOWN").build());
    }

    private long parseLong(Object obj) {
        return obj != null ? Long.parseLong(obj.toString()) : 0L;
    }
}
