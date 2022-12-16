/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.bucket4j.redis.lettuce.cas;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.redis.AbstractRedisProxyManagerBuilder;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LettuceClusterProxyManager extends AbstractCompareAndSwapBasedProxyManager<byte[]> {
	
	private final ExpirationAfterWriteStrategy expirationStrategy;
	
	private final RedisAdvancedClusterCommands<String, String> commands;
	
	private static final String SET_KEY_NOT_ALREADY_EXIST_SCRIPT = "return redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2])";
	
	private static final String UPDATE_DATA_AND_EXPIRE_IF_KEY_EXIST = "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
		"redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2]); " +
		"return 1; " +
		"else " +
		"return 0; " +
		"end";
	
	private static final String LIMITLESS_SET_DATA_IF_KEY_EXIST = "return redis.call('SET', KEYS[1], ARGV[1], 'NX')";
	
	private static final String LIMITLESS_SET_KEY_NOT_ALREADY_EXIST = "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
		"redis.call('SET', KEYS[1], ARGV[2]); " +
		"return 1; " +
		"else " +
		"return 0; " +
		"end";
	
	protected LettuceClusterProxyManager(LettuceClusterProxyManagerBuilder builder) {
		super(builder.getClientSideConfig());
		this.expirationStrategy = builder.getNotNullExpirationStrategy();
		this.commands = builder.commands;
	}
	
	public static LettuceClusterProxyManagerBuilder builderFor(RedisAdvancedClusterCommands<String, String> commands) {
		return new LettuceClusterProxyManagerBuilder(commands);
	}
	
	public static class LettuceClusterProxyManagerBuilder extends AbstractRedisProxyManagerBuilder<LettuceClusterProxyManagerBuilder> {
		private final RedisAdvancedClusterCommands<String, String> commands;
		
		private LettuceClusterProxyManagerBuilder(RedisAdvancedClusterCommands<String, String> commands) {
			this.commands = Objects.requireNonNull(commands);
		}
		
		public LettuceClusterProxyManager build() {
			return new LettuceClusterProxyManager(this);
		}
	}
	
	@Override
	protected CompareAndSwapOperation beginCompareAndSwapOperation(byte[] key) {
		return new CompareAndSwapOperation() {
			@Override
			public Optional<byte[]> getStateData() {
				String cacheData = commands.get(new String(key, StandardCharsets.UTF_8));
				
				if (Objects.isNull(cacheData)) {
					return Optional.empty();
				}
				
				byte[] stateFuture = stringToByteDecode(cacheData);
				
				return Optional.of(stateFuture);
			}
			
			@Override
			public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
				return compareAndSwapFuture(key, originalData, newData, newState);
			}
		};
	}
	
	private Boolean compareAndSwapFuture(byte[] key, byte[] originalData, byte[] newData, RemoteBucketState newState) {
		long ttlMillis = calculateTtlMillis(newState);
		
		String originalDataString = null;
		String newDataString = null;
		
		if (Objects.nonNull(originalData)) {
			originalDataString = byteToStringEncode(originalData);
		}
		
		if (Objects.nonNull(newData)) {
			newDataString = byteToStringEncode(newData);
		}
		
		if (ttlMillis > 0) {
			if (originalData == null) {
				return commands.eval(SET_KEY_NOT_ALREADY_EXIST_SCRIPT, ScriptOutputType.BOOLEAN,
						byteToStringArray(key), newDataString, String.valueOf(ttlMillis));
			} else {
				return commands.eval(UPDATE_DATA_AND_EXPIRE_IF_KEY_EXIST, ScriptOutputType.BOOLEAN,
						byteToStringArray(key), originalDataString, newDataString, String.valueOf(ttlMillis));
			}
		} else {
			if (originalData == null) {
				return commands.eval(LIMITLESS_SET_DATA_IF_KEY_EXIST, ScriptOutputType.BOOLEAN,
						byteToStringArray(key), newDataString);
			} else {
				return commands.eval(LIMITLESS_SET_KEY_NOT_ALREADY_EXIST, ScriptOutputType.BOOLEAN,
						byteToStringArray(key), originalDataString, newDataString);
			}
		}
	}
	
	private String[] byteToStringArray(byte[] data) {
		return new String[]{new String(data, StandardCharsets.UTF_8)};
	}
	
	private byte[] stringToByteDecode(String cacheData) {
		return Base64.getDecoder().decode(cacheData);
	}
	
	private String byteToStringEncode(byte[] byteData) {
		return Base64.getEncoder().encodeToString(byteData);
	}
	
	private long calculateTtlMillis(RemoteBucketState state) {
		Optional<TimeMeter> clock = getClientSideConfig().getClientSideClock();
		
		long currentTimeNanos = clock.map(TimeMeter::currentTimeNanos)
										.orElseGet(() -> System.currentTimeMillis() * 1_000_000);
		
		return expirationStrategy.calculateTimeToLiveMillis(state, currentTimeNanos);
	}
	
	@Override
	protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(byte[] key) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected CompletableFuture<Void> removeAsync(byte[] key) {
		return null;
	}
	
	@Override
	public void removeProxy(byte[] key) {
		// method is empty
	}
	
	@Override
	public boolean isAsyncModeSupported() {
		return false;
	}
}
