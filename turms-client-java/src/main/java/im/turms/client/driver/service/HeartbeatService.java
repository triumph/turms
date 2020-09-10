/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
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
 */

package im.turms.client.driver.service;

import im.turms.client.driver.StateStore;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import okio.ByteString;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * @author James Chen
 */
public class HeartbeatService {

    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(120);

    private final StateStore stateStore;

    private final Duration heartbeatInterval;
    private final Duration minRequestsInterval;

    private final ScheduledExecutorService heartbeatTimer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatFuture;

    private final ConcurrentLinkedQueue<CompletableFuture<Void>> heartbeatCallbacks = new ConcurrentLinkedQueue<>();

    public HeartbeatService(StateStore stateStore, Duration minRequestsInterval, Duration heartbeatInterval) {
        this.stateStore = stateStore;
        this.minRequestsInterval = minRequestsInterval;
        this.heartbeatInterval = heartbeatInterval != null ? heartbeatInterval : HEARTBEAT_INTERVAL;
    }

    public void start() {
        if (heartbeatFuture == null) {
            heartbeatFuture = heartbeatTimer.scheduleAtFixedRate(
                    (this::checkAndSendHeartbeatTask),
                    heartbeatInterval.toMillis(),
                    heartbeatInterval.toMillis(),
                    TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (heartbeatFuture != null && !heartbeatFuture.isCancelled() && !heartbeatFuture.isDone()) {
            heartbeatFuture.cancel(true);
        }
    }

    public CompletableFuture<Void> send() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (stateStore.isConnected()) {
            boolean wasEnqueued = stateStore.getWebSocket().send(ByteString.EMPTY);
            if (wasEnqueued) {
                heartbeatCallbacks.offer(future);
            } else {
                future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
        return future;
    }

    public void reset() {
        stop();
        start();
    }

    public void notifyHeartbeatCallbacks() {
        while (!heartbeatCallbacks.isEmpty()) {
            heartbeatCallbacks.poll().complete(null);
        }
    }

    public void rejectHeartbeatCallbacks(Throwable throwable) {
        while (!heartbeatCallbacks.isEmpty()) {
            heartbeatCallbacks.poll().completeExceptionally(throwable);
        }
    }

    private void checkAndSendHeartbeatTask() {
        long difference = System.currentTimeMillis() - stateStore.getLastRequestDate();
        if (difference > minRequestsInterval.toMillis()) {
            send();
        }
    }

}