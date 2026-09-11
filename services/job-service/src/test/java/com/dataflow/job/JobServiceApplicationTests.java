package com.dataflow.job;

import com.dataflow.job.dto.CreateJobRequest;
import com.dataflow.job.dto.JobDto;
import com.dataflow.job.dto.JobExecutionDto;
import com.dataflow.job.service.JobService;
import com.dataflow.job.service.RedisLockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JobServiceApplicationTests {

    @Autowired
    private JobService jobService;

    @Autowired
    private RedisLockService redisLockService;

    @Test
    void contextLoads() {
        assertNotNull(jobService);
        assertNotNull(redisLockService);
    }

    @Test
    void testRedisDistributedLockMechanism() {
        String lockKey = "test:lock:job:999";
        String lockVal1 = "instance-1";
        String lockVal2 = "instance-2";

        boolean acquired1 = redisLockService.tryLock(lockKey, lockVal1, Duration.ofMinutes(1));
        assertTrue(acquired1, "Instance 1 should acquire the lock");

        boolean acquired2 = redisLockService.tryLock(lockKey, lockVal2, Duration.ofMinutes(1));
        assertFalse(acquired2, "Instance 2 should fail to acquire lock while held by Instance 1");

        boolean unlocked = redisLockService.unlock(lockKey, lockVal1);
        assertTrue(unlocked, "Instance 1 should release lock successfully");

        boolean acquiredAgain = redisLockService.tryLock(lockKey, lockVal2, Duration.ofMinutes(1));
        assertTrue(acquiredAgain, "Instance 2 should now be able to acquire lock");

        redisLockService.unlock(lockKey, lockVal2);
    }

    @Test
    void testJobCreationAndTriggerExecution() {
        CreateJobRequest request = CreateJobRequest.builder()
                .name("SYNC_CUSTOMER_DATA")
                .cronExpression("0 */5 * * * *")
                .targetService("customer-service")
                .targetEndpoint("/api/customers/sync")
                .httpMethod("POST")
                .active(true)
                .maxRetries(3)
                .build();

        JobDto jobDto = jobService.createJob(request);
        assertNotNull(jobDto);
        assertNotNull(jobDto.getId());
        assertEquals("SYNC_CUSTOMER_DATA", jobDto.getName());

        JobExecutionDto execution = jobService.triggerJob(jobDto.getId());
        assertNotNull(execution);
        assertEquals("SUCCESS", execution.getStatus());

        List<JobExecutionDto> history = jobService.getJobExecutions(jobDto.getId());
        assertFalse(history.isEmpty());
        assertEquals("SUCCESS", history.get(0).getStatus());
    }
}
