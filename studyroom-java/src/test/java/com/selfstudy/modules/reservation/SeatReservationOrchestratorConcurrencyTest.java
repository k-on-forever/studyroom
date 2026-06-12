package com.selfstudy.modules.reservation;

import com.selfstudy.config.ReservationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 并发预约测试 — 需要本地 Redis 运行（127.0.0.1:6379，无密码）。
 * 跳过方式：gradle test -x *ConcurrencyTest 或 IDE 排除 @Tag("concurrency")
 */
@Tag("concurrency")
class SeatReservationOrchestratorConcurrencyTest {

	private SeatReservationOrchestrator orchestrator;
	private SeatBitmapRedisService bitmap;
	private ReservationDistributedLock lockObj;
	private StringRedisTemplate template;

	@BeforeEach
	void setUp() {
		// 连接 Redis（按 application.yml 配置；连不上则跳过 concurrency 测试）
		LettuceConnectionFactory factory = new LettuceConnectionFactory("192.168.238.138", 6379);
		factory.setDatabase(0);
		factory.setPassword("123321");
		factory.afterPropertiesSet();
		template = new StringRedisTemplate(factory);
		template.afterPropertiesSet();

		// Redisson
		Config c = new Config();
		c.useSingleServer().setAddress("redis://192.168.238.138:6379").setDatabase(0).setPassword("123321");
		RedissonClient redisson = Redisson.create(c);

		// 配置
		ReservationProperties props = new ReservationProperties();
		props.setSlotMinutes(10);
		props.setDayStart("08:00");
		props.setDayEnd("22:30");
		props.setLockWaitSeconds(3);
		props.setLockLeaseSeconds(10);
		props.setMinBookingMinutes(60);

		bitmap = new SeatBitmapRedisService(template, props);
		lockObj = new ReservationDistributedLock(redisson, props);
		orchestrator = new SeatReservationOrchestrator(lockObj, bitmap);
	}

	@Test
	void concurrentBooking_sameSeatSameSlot_onlyOneSucceeds() throws Exception {
		Long seatId = 999L;
		String bizDate = "2025-07-20";
		int slotStart = 6;   // 09:00
		int slotEnd = 12;    // 11:00

		// 清理旧数据
		bitmap.releaseRange(seatId, bizDate, 0, 87);

		int threadCount = 50;
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		for (int i = 0; i < threadCount; i++) {
			pool.submit(() -> {
				try {
					latch.countDown();
					latch.await(); // 同时出发
					boolean ok = orchestrator.tryReserve(seatId, bizDate, slotStart, slotEnd);
					if (ok) successCount.incrementAndGet();
				} catch (Throwable e) {
					errors.add(e);
				}
			});
		}
		pool.shutdown();
		while (!pool.isTerminated()) {
			Thread.sleep(100);
		}

		System.out.println("成功数：" + successCount.get() + " / " + threadCount);
		errors.forEach(e -> System.out.println("异常：" + e.getMessage()));

		// 核心断言：10 个并发只有 1 个能占到位
		assertEquals(1, successCount.get(), "并发预约同一座位同一时段，应只有 1 人成功");

		// 清理
		bitmap.releaseRange(seatId, bizDate, 0, 87);
	}

	@Test
	void concurrentBooking_differentSlots_allSucceed() throws Exception {
		Long seatId = 998L;
		String bizDate = "2025-07-20";

		bitmap.releaseRange(seatId, bizDate, 0, 87);

		int threadCount = 5;
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);

		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		for (int i = 0; i < threadCount; i++) {
			int start = 6 + i * 3; // 每人不同时段
			int end = start + 3;
			pool.submit(() -> {
				try {
					latch.countDown();
					latch.await();
					boolean ok = orchestrator.tryReserve(seatId, bizDate, start, end);
					if (ok) successCount.incrementAndGet();
				} catch (Throwable ignored) {
				}
			});
		}
		pool.shutdown();
		while (!pool.isTerminated()) {
			Thread.sleep(100);
		}

		System.out.println("成功数：" + successCount.get() + " / " + threadCount);
		// 不同时段都应该成功
		assertEquals(threadCount, successCount.get());

		bitmap.releaseRange(seatId, bizDate, 0, 87);
	}

	@Test
	void concurrentCancelAndBook_doesNotOverbook() throws Exception {
		Long seatId = 997L;
		String bizDate = "2025-07-20";
		int slotStart = 12; // 10:00
		int slotEnd = 15;   // 10:30

		bitmap.releaseRange(seatId, bizDate, 0, 87);

		// 先占用
		orchestrator.tryReserve(seatId, bizDate, slotStart, slotEnd);

		int threadCount = 5;
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);

		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		// 第一个线程取消，其余 4 个同时抢
		pool.submit(() -> {
			try {
				latch.countDown();
				latch.await();
				bitmap.releaseRange(seatId, bizDate, slotStart, slotEnd);
			} catch (Throwable ignored) {
			}
		});
		for (int i = 0; i < threadCount - 1; i++) {
			pool.submit(() -> {
				try {
					latch.countDown();
					latch.await();
					boolean ok = orchestrator.tryReserve(seatId, bizDate, slotStart, slotEnd);
					if (ok) successCount.incrementAndGet();
				} catch (Throwable ignored) {
				}
			});
		}
		pool.shutdown();
		while (!pool.isTerminated()) {
			Thread.sleep(100);
		}

		System.out.println("取消并发重抢 成功数：" + successCount.get());
		// 取消+重抢：最终只能有 1 人成功（可能有 0 人——如果取消还没执行就被抢走）
		// 但不可能超过 1
		assertEquals(1, successCount.get(),
			"取消与并发预约同一座位时段，最终 bitmap 不应被重复占用");

		bitmap.releaseRange(seatId, bizDate, 0, 87);
	}
}
