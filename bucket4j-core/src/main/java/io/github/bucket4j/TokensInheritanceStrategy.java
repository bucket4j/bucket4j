/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
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
package io.github.bucket4j;

/**
 * Specifies the rules for inheritance of available tokens when {@link Bucket#replaceConfiguration(BucketConfiguration, TokensInheritanceStrategy)} happens.
 */
public enum TokensInheritanceStrategy {

    /**
     * Makes to copy available tokens proportional to bandwidth capacity by following formula:
     * <code>
     *     newAvailableTokens = availableTokensBeforeReplacement * (newBandwidthCapacity / capacityBeforeReplacement)
     * </code>
     *
     * <p>
     * Let's describe few examples.
     *
     * <p> <b>Example 1:</b> imagine bandwidth that was created by {@code Bandwidth.classic(100, Refill.gready(10, Duration.ofMinutes(1)))}.
     * At the moment of config replacement it was 40 available tokens. After replacing this bandwidth by following {@code Bandwidth.classic(200, Refill.gready(10, Duration.ofMinutes(1)))}
     * 40 available tokens will be multiplied by 2(200/100), and after replacement we will have 80 available tokens.
     *
     * <p> <b>Example 1:</b> imagine bandwidth that was created by {@code Bandwidth.classic(100, Refill.gready(10, Duration.ofMinutes(1)))}.
     * At the moment of config replacement it was 40 available tokens. After replacing this bandwidth by following {@code Bandwidth.classic(20, Refill.gready(10, Duration.ofMinutes(1)))}
     * 40 available tokens will be multiplied by 0.2(20/100), and after replacement we will have 8 available tokens.
     */
    PROPORTIONALLY((byte) 0),

    /**
     * Instructs to copy available tokens as is, but with one exclusion: if available tokens is greater than new capacity,
     * available tokens will be decreased to new capacity.
     *
     * <p>
     * Let's describe few examples.
     *
     * <p> <b>Example 1:</b> imagine bandwidth that was created by {@code Bandwidth.classic(100, Refill.gready(10, Duration.ofMinutes(1)))}.
     * At the moment of config replacement it was 40 available tokens. After replacing this bandwidth by following {@code Bandwidth.classic(200, Refill.gready(10, Duration.ofMinutes(1)))}
     * 40 available tokens will be just copied, and after replacement we will have 40 available tokens.
     *
     * <p> <b>Example 1:</b> imagine bandwidth that was created by {@code Bandwidth.classic(100, Refill.gready(10, Duration.ofMinutes(1)))}.
     * At the moment of config replacement it was 40 available tokens. After replacing this bandwidth by following {@code Bandwidth.classic(20, Refill.gready(10, Duration.ofMinutes(1)))}
     * 40 available tokens can not be copied as is, because it is greater then new capacity, so available tokens will be reduced to 20.
     */
    AS_IS((byte) 1),

    /**
     * Use this mode when you want just to forget about previous bucket state.
     * {@code bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET)} just erases all previous state.
     * Using this strategy equals to removing bucket and creating again with new configuration.
     */
    RESET((byte) 2),

    /**
     * Instructs to copy available tokens as is, but with one exclusion: if new bandwidth capacity is greater than old capacity, available tokens will be increased by the difference between the old and the new configuration.
     *
     * <p>
     * The formula is <code>newAvailableTokens = Math.min(availableTokensBeforeReplacement, newBandwidthCapacity) + Math.max(0, newBandwidthCapacity - capacityBeforeReplacement)</code>
     *
     * <p>
     * Let's describe few examples.
     *
     * <p>
     *     <b>Example 1:</b> imagine bandwidth that was created by {@code Bandwidth.classic(100, Refill.gready(10, Duration.ofMinutes(1)))}.
     *     At the moment of configuration replacement, it was 40 available tokens.
     *     After replacing this bandwidth by following {@code Bandwidth.classic(200, Refill.gready(10, Duration.ofMinutes(1)))} 40 available tokens will be copied and added to the difference between old and new configuration,
     *     and after replacement, we will have 140 available tokens.
     *
     * <p>
     *     <b>Example 2:</b> imagine bandwidth that was created by {@code Bandwidth.classic(100, Refill.gready(10, Duration.ofMinutes(1)))}.
     *     At the moment of config replacement it was 40 available tokens.
     *     After replacing this bandwidth by following {@codeBandwidth.classic(20, Refill.gready(10, Duration.ofMinutes(1))))},
     *     and after replacement we will have 20 available tokens.
     *
     * Example 3: imagine bandwidth that was created by {@code Bandwidth.classic(100, Refill.gready(10, Duration.ofMinutes(1)))}. At the moment of config replacement it was 10 available tokens. After replacing this bandwidth by following Bandwidth.classic(20, Refill.gready(10, Duration.ofMinutes(1)))), and after replacement we will have 10 available tokens.
     */
    ADDITIVE((byte) 3)

    ;

    private final byte id;

    TokensInheritanceStrategy(byte id) {
        this.id = id;
    }

    private static final TokensInheritanceStrategy[] modes = new TokensInheritanceStrategy[] {
            PROPORTIONALLY, AS_IS, RESET
    };

    public static TokensInheritanceStrategy getById(byte id) {
        return modes[id];
    }

    public byte getId() {
        return id;
    }

}
