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

package im.turms.turms.service.user.onlineuser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.hazelcast.cluster.Member;
import im.turms.common.TurmsCloseStatus;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.annotation.constraint.DeviceTypeConstraint;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ReactorUtil;
import im.turms.turms.common.TrivialTaskService;
import im.turms.turms.constant.CloseStatusFactory;
import im.turms.turms.plugin.TurmsPluginManager;
import im.turms.turms.plugin.UserOnlineStatusChangeHandler;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.pojo.domain.UserLoginLog;
import im.turms.turms.pojo.domain.UserOnlineUserNumber;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.service.user.UserLocationService;
import im.turms.turms.service.user.UserLoginLogService;
import im.turms.turms.task.*;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.math.MathFlux;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static im.turms.turms.cluster.TurmsClusterManager.HASH_SLOTS_NUMBER;
import static im.turms.turms.common.Constants.ALL_DEVICE_TYPES;

@Service
@Validated
public class OnlineUserService {
    private static final int DEFAULT_ONLINE_USERS_MANAGER_CAPACITY = 1024;
    private static final String LOG_ONLINE_USERS_NUMBER_TASK = "loun";
    private static final UserLocation EMPTY_USER_LOCATION = new UserLocation(null, null, null, null, null, null, null, null);
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPluginManager turmsPluginManager;
    private final UsersNearbyService usersNearbyService;
    private final UserLoginLogService userLoginLogService;
    private final UserLocationService userLocationService;
    private final HashedWheelTimer heartbeatTimer;
    private final TrivialTaskService onlineUsersNumberPersisterTimer;
    private final TurmsTaskExecutor turmsTaskExecutor;
    /**
     * Integer(Slot) -> Long(userId) -> OnlineUserManager
     */
    private final List<Map<Long, OnlineUserManager>> onlineUsersManagerAtSlots;
    /**
     * Pair<user ID, session ID> -> CloseStatus
     */
    private final Cache<Pair<Long, String>, Integer> disconnectionReasonCache;

    public OnlineUserService(
            TurmsClusterManager turmsClusterManager,
            UsersNearbyService usersNearbyService,
            ReactiveMongoTemplate mongoTemplate,
            UserLoginLogService userLoginLogService,
            UserLocationService userLocationService,
            TurmsTaskExecutor turmsTaskExecutor,
            TurmsPluginManager turmsPluginManager,
            TrivialTaskService trivialTaskService) {
        this.turmsClusterManager = turmsClusterManager;
        this.usersNearbyService = usersNearbyService;
        turmsClusterManager.addListenerOnMembersChange(
                membershipEvent -> {
                    onClusterMembersChange();
                    return null;
                });
        this.mongoTemplate = mongoTemplate;
        this.userLoginLogService = userLoginLogService;
        this.userLocationService = userLocationService;
        this.turmsTaskExecutor = turmsTaskExecutor;
        this.turmsPluginManager = turmsPluginManager;
        this.heartbeatTimer = new HashedWheelTimer();
        this.onlineUsersNumberPersisterTimer = trivialTaskService;
        this.disconnectionReasonCache = Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
        this.onlineUsersManagerAtSlots = new ArrayList(Arrays.asList(new HashMap[HASH_SLOTS_NUMBER]));
        rescheduleOnlineUsersNumberPersister();
        TurmsProperties.addListeners(turmsProperties -> {
            rescheduleOnlineUsersNumberPersister();
            return null;
        });
    }

    private void rescheduleOnlineUsersNumberPersister() {
        onlineUsersNumberPersisterTimer.reschedule(LOG_ONLINE_USERS_NUMBER_TASK,
                turmsClusterManager.getTurmsProperties().getUser().getOnlineUsersNumberPersisterCron(),
                this::checkAndSaveOnlineUsersNumber);
    }

    public void setAllLocalUsersOffline(@NotNull CloseStatus closeStatus) {
        for (Map<Long, OnlineUserManager> managersMap : onlineUsersManagerAtSlots) {
            if (managersMap != null) {
                setManagersOffline(closeStatus, managersMap.values());
                managersMap.clear();
            }
        }
        onlineUsersManagerAtSlots.clear();
    }

    public void setIrresponsibleUsersOffline(boolean isServerClosing) {
        for (int index = 0; index < HASH_SLOTS_NUMBER; index++) {
            Member member = turmsClusterManager.getClusterMemberBySlotIndex(index);
            if (member != null) {
                if (!member.equals(turmsClusterManager.getLocalMember())) {
                    String host = String.format("%s:%s", member.getAddress().getHost(), member.getAttribute("PORT"));
                    TurmsCloseStatus status = isServerClosing ? TurmsCloseStatus.SERVER_CLOSED : TurmsCloseStatus.REDIRECT;
                    setUsersOfflineBySlotIndex(index, CloseStatusFactory.get(status, host));
                }
            } else {
                setUsersOfflineBySlotIndex(index, CloseStatusFactory.get(TurmsCloseStatus.SERVER_ERROR, "Cannot find a server responsible for the user"));
            }
        }
    }

    public void setUsersOfflineBySlotIndex(@NotNull Integer slotIndex, @NotNull CloseStatus closeStatus) {
        if (slotIndex >= 0 && slotIndex < HASH_SLOTS_NUMBER) {
            Map<Long, OnlineUserManager> managerMap = getOnlineUsersManager(slotIndex);
            if (managerMap != null) {
                setManagersOffline(closeStatus, managerMap.values());
            }
        }
    }

    public boolean setLocalUserDevicesOffline(
            @NotNull Long userId,
            @NotEmpty Set<@DeviceTypeConstraint DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        OnlineUserManager manager = getLocalOnlineUserManager(userId);
        if (manager != null) {
            Date now = new Date();
            for (DeviceType deviceType : deviceTypes) {
                OnlineUserManager.Session session = manager.getSession(deviceType);
                if (session != null) {
                    clearSession(userId, session, now, closeStatus);
                }
            }
            manager.setSpecificDevicesOffline(deviceTypes, closeStatus);
            if (manager.getSessionsNumber() == 0) {
                clearOnlineUserManager(userId);
            }
            if (turmsClusterManager.getTurmsProperties().getPlugin().isEnabled()) {
                List<UserOnlineStatusChangeHandler> handlerList = turmsPluginManager.getUserOnlineStatusChangeHandlerList();
                if (!handlerList.isEmpty()) {
                    for (UserOnlineStatusChangeHandler handler : handlerList) {
                        handler.goOffline(manager, closeStatus).subscribe();
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void clearSession(Long userId, OnlineUserManager.Session session, Date date, CloseStatus closeStatus) {
        Long logId = session.getLogId();
        if (logId != null) {
            userLoginLogService
                    .updateLogoutDate(logId, date)
                    .subscribe();
        }
        session.getNotificationSink().complete();
        usersNearbyService.removeUserLocation(userId, session.getDeviceType());
        disconnectionReasonCache.put(Pair.of(userId, session.getWebSocketSession().getId()),
                closeStatus.getCode());
    }

    private void setManagersOffline(@NotNull CloseStatus closeStatus, @NotNull Collection<OnlineUserManager> managers) {
        for (OnlineUserManager manager : managers) {
            if (manager != null) {
                ConcurrentMap<DeviceType, OnlineUserManager.Session> sessionMap = manager.getOnlineUserInfo().getSessionMap();
                Date now = new Date();
                Long userId = manager.getOnlineUserInfo().getUserId();
                for (OnlineUserManager.Session session : sessionMap.values()) {
                    clearSession(userId, session, now, closeStatus);
                }
                manager.setAllDevicesOffline(closeStatus);
                if (manager.getSessionsNumber() == 0) {
                    clearOnlineUserManager(userId);
                }
                if (turmsClusterManager.getTurmsProperties().getPlugin().isEnabled()) {
                    List<UserOnlineStatusChangeHandler> handlerList = turmsPluginManager.getUserOnlineStatusChangeHandlerList();
                    if (!handlerList.isEmpty()) {
                        for (UserOnlineStatusChangeHandler handler : handlerList) {
                            handler.goOffline(manager, closeStatus).subscribe();
                        }
                    }
                }
            }
        }
    }

    public void clearOnlineUserManager(@NotNull Long userId) {
        int slotIndex = turmsClusterManager.getSlotIndexByUserId(userId);
        onlineUsersManagerAtSlots.get(slotIndex).remove(userId);
    }

    public boolean setLocalUserOffline(
            @NotNull Long userId,
            @NotNull CloseStatus closeStatus) {
        return setLocalUserDevicesOffline(userId, ALL_DEVICE_TYPES, closeStatus);
    }

    public boolean setLocalUserDeviceOffline(
            @NotNull Long userId,
            @NotNull @DeviceTypeConstraint DeviceType deviceType,
            @NotNull CloseStatus closeStatus) {
        return setLocalUserDevicesOffline(userId, Collections.singleton(deviceType), closeStatus);
    }

    public Mono<Boolean> setUsersOffline(
            @NotEmpty Set<Long> userIds,
            @NotNull CloseStatus closeStatus) {
        List<Mono<Boolean>> list = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            list.add(setUserOffline(userId, closeStatus));
        }
        return Flux.merge(list).all(value -> value);
    }

    public Mono<Boolean> setUserOffline(
            @NotNull Long userId,
            @NotNull CloseStatus closeStatus) {
        boolean responsible = turmsClusterManager.isCurrentNodeResponsibleByUserId(userId);
        if (responsible) {
            setLocalUserOffline(userId, closeStatus);
            return Mono.just(true);
        } else {
            Member member = turmsClusterManager.getMemberByUserId(userId);
            if (member != null) {
                Future<Boolean> future = turmsClusterManager
                        .getExecutor()
                        .submitToMember(new SetUserOfflineTask(userId, null, closeStatus.getCode()), member);
                return ReactorUtil.future2Mono(future);
            } else {
                return Mono.just(false);
            }
        }
    }

    public Mono<Boolean> setUsersDevicesOffline(
            @NotEmpty Set<Long> userIds,
            @NotEmpty Set<@DeviceTypeConstraint DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        List<Mono<Boolean>> monos = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            monos.add(setUserDevicesOffline(userId, deviceTypes, closeStatus));
        }
        return Flux.merge(monos).all(value -> value);
    }

    public Mono<Boolean> setUserDevicesOffline(
            @NotNull Long userId,
            @NotEmpty Set<@DeviceTypeConstraint DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        boolean responsible = turmsClusterManager.isCurrentNodeResponsibleByUserId(userId);
        if (responsible) {
            setLocalUserDevicesOffline(userId, deviceTypes, closeStatus);
            return Mono.just(true);
        } else {
            Member member = turmsClusterManager.getMemberByUserId(userId);
            if (member != null) {
                Set<Integer> types = new HashSet<>(deviceTypes.size());
                for (DeviceType deviceType : deviceTypes) {
                    types.add(deviceType.ordinal());
                }
                Future<Boolean> future = turmsClusterManager
                        .getExecutor()
                        .submitToMember(new SetUserOfflineTask(userId, types, closeStatus.getCode()), member);
                return ReactorUtil.future2Mono(future);
            } else {
                return Mono.just(false);
            }
        }
    }

    public int countLocalOnlineUsers() {
        int number = 0;
        for (Map<Long, OnlineUserManager> managersMap : onlineUsersManagerAtSlots) {
            if (managersMap != null) {
                number += managersMap.size();
            }
        }
        return number;
    }

    public Mono<Integer> countOnlineUsers() {
        Flux<Integer> futures = turmsTaskExecutor.callAll(new CountOnlineUsersTask(), Duration.ofSeconds(30));
        return MathFlux.sumInt(futures);
    }

    public void resetHeartbeatTimeout(
            @NotNull Long userId,
            @NotNull @DeviceTypeConstraint DeviceType deviceType) {
        OnlineUserManager onlineUserManager = getLocalOnlineUserManager(userId);
        if (onlineUserManager != null) {
            OnlineUserManager.Session session = onlineUserManager.getSession(deviceType);
            if (session != null) {
                int minInterval = turmsClusterManager.getTurmsProperties().getSession().getMinHeartbeatRefreshIntervalSeconds();
                long now = System.currentTimeMillis();
                int interval = (int) ((now - session.getLastHeartbeatTimestamp()) / 1000);
                if (interval >= minInterval) {
                    boolean success = session.getHeartbeatTimeout().cancel();
                    if (success) {
                        Timeout heartbeatTimeout = newHeartbeatTimeout(userId, deviceType);
                        session.setHeartbeatTimeout(heartbeatTimeout);
                        session.setLastHeartbeatTimestamp(now);
                    }
                }
            }
        }
    }

    private Timeout newHeartbeatTimeout(@NotNull Long userId, @NotNull @DeviceTypeConstraint DeviceType deviceType) {
        return heartbeatTimer.newTimeout(
                timeout -> setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(TurmsCloseStatus.HEARTBEAT_TIMEOUT)),
                turmsClusterManager.getTurmsProperties().getSession().getRequestHeartbeatTimeoutSeconds(),
                TimeUnit.SECONDS);
    }

    public void checkAndSaveOnlineUsersNumber() {
        if (turmsClusterManager.isCurrentMemberMaster()) {
            countOnlineUsers()
                    .flatMap(this::saveOnlineUsersNumber)
                    .subscribe();
        }
    }

    public Mono<UserOnlineUserNumber> saveOnlineUsersNumber(@NotNull Integer onlineUsersNumber) {
        UserOnlineUserNumber userOnlineUserNumber = new UserOnlineUserNumber(new Date(), onlineUsersNumber);
        return mongoTemplate.save(userOnlineUserNumber);
    }

    private Mono<UserLoginLog> logUserOnline(
            @NotNull Long userId,
            @NotNull Integer ip,
            @NotNull @DeviceTypeConstraint DeviceType usingDeviceType,
            @Nullable Map<String, String> deviceDetails,
            @Nullable Long locationId) {
        return userLoginLogService.save(userId, ip, usingDeviceType, deviceDetails, locationId);
    }

    public Mono<TurmsStatusCode> addOnlineUser(
            @NotNull Long userId,
            @NotNull UserStatus userStatus,
            @NotNull DeviceType loggingInDeviceType,
            @Nullable Map<String, String> deviceDetails,
            @NotNull Integer ip,
            @Nullable PointFloat userLocation,
            @NotNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> notificationSink) {
        Mono<UserLocation> locationIdMono;
        if (userLocation != null) {
            locationIdMono = userLocationService
                    .saveUserLocation(null, userId, loggingInDeviceType, userLocation.xFloat(), userLocation.yFloat(), new Date());
        } else {
            locationIdMono = Mono.empty();
        }
        return locationIdMono
                .defaultIfEmpty(EMPTY_USER_LOCATION)
                .flatMap(location -> {
                    Long locationId = null;
                    if (EMPTY_USER_LOCATION != location) {
                        locationId = location.getId();
                    }
                    if (turmsClusterManager.getTurmsProperties().getLog().isLogUserLogin()) {
                        return logUserOnline(userId, ip, loggingInDeviceType, deviceDetails, locationId)
                                .map(log -> {
                                    if (turmsClusterManager.getTurmsProperties().getPlugin().isEnabled()) {
                                        userLoginLogService.triggerLogHandlers(log);
                                    }
                                    return setUpOnlineUserManager(userId, loggingInDeviceType, userStatus, location, webSocketSession, notificationSink, log.getId());
                                })
                                .onErrorReturn(TurmsStatusCode.FAILED);
                    } else {
                        if (turmsClusterManager.getTurmsProperties().getPlugin().isEnabled()) {
                            userLoginLogService.triggerLogHandlers(userId, ip, loggingInDeviceType, deviceDetails, locationId);
                        }
                        return Mono.just(setUpOnlineUserManager(userId, loggingInDeviceType, userStatus, location, webSocketSession, notificationSink, null));
                    }
                });
    }

    private TurmsStatusCode setUpOnlineUserManager(
            @NotNull Long userId,
            @NotNull DeviceType loggingInDeviceType,
            @NotNull UserStatus userStatus,
            @NotNull UserLocation location,
            @NotNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> notificationSink,
            @Nullable Long logId) {
        Integer slotIndex = turmsClusterManager.getSlotIndexByUserIdForCurrentNode(userId);
        if (slotIndex == null) {
            return TurmsStatusCode.NOT_RESPONSIBLE;
        } else {
            OnlineUserManager onlineUserManager = getLocalOnlineUserManager(userId);
            Timeout heartbeatTimeout = newHeartbeatTimeout(userId, loggingInDeviceType);
            if (onlineUserManager != null) {
                onlineUserManager.setUserOnlineStatus(
                        userStatus == UserStatus.OFFLINE || userStatus == UserStatus.UNRECOGNIZED ?
                                UserStatus.AVAILABLE : userStatus);
                onlineUserManager.setDeviceTypeOnline(
                        loggingInDeviceType,
                        location == EMPTY_USER_LOCATION ? null : location,
                        webSocketSession,
                        notificationSink,
                        heartbeatTimeout,
                        logId);
            } else {
                onlineUserManager = new OnlineUserManager(
                        userId,
                        userStatus,
                        loggingInDeviceType,
                        location == EMPTY_USER_LOCATION ? null : location,
                        webSocketSession,
                        notificationSink,
                        heartbeatTimeout,
                        logId);
            }
            getOrAddOnlineUsersManager(slotIndex).put(userId, onlineUserManager);
            if (turmsClusterManager.getTurmsProperties().getPlugin().isEnabled()) {
                List<UserOnlineStatusChangeHandler> handlerList = turmsPluginManager.getUserOnlineStatusChangeHandlerList();
                if (!handlerList.isEmpty()) {
                    for (UserOnlineStatusChangeHandler handler : handlerList) {
                        handler.goOnline(onlineUserManager, loggingInDeviceType).subscribe();
                    }
                }
            }
            return TurmsStatusCode.OK;
        }
    }

    private Map<Long, OnlineUserManager> getOrAddOnlineUsersManager(@NotNull Integer slotIndex) {
        if (slotIndex < 0 || slotIndex >= HASH_SLOTS_NUMBER) {
            throw new IllegalArgumentException();
        }
        Map<Long, OnlineUserManager> userManagerMap = onlineUsersManagerAtSlots.get(slotIndex);
        if (userManagerMap == null) {
            Map<Long, OnlineUserManager> map = new HashMap<>(DEFAULT_ONLINE_USERS_MANAGER_CAPACITY);
            onlineUsersManagerAtSlots.set(slotIndex, map);
            return map;
        } else {
            return userManagerMap;
        }
    }

    private Map<Long, OnlineUserManager> getOnlineUsersManager(@NotNull Integer slotIndex) {
        if (slotIndex >= 0 && slotIndex < onlineUsersManagerAtSlots.size()) {
            return onlineUsersManagerAtSlots.get(slotIndex);
        } else {
            return null;
        }
    }

    public OnlineUserManager getLocalOnlineUserManager(@NotNull Long userId) {
        Integer slotIndex = turmsClusterManager.getSlotIndexByUserIdForCurrentNode(userId);
        if (slotIndex != null) {
            Map<Long, OnlineUserManager> managersMap = getOrAddOnlineUsersManager(slotIndex);
            return managersMap.get(userId);
        } else {
            return null;
        }
    }

    public Mono<Boolean> updateOnlineUsersStatus(@NotEmpty Set<Long> userIds, @NotNull UserStatus userStatus) {
        List<Mono<Boolean>> monos = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            monos.add(updateOnlineUserStatus(userId, userStatus));
        }
        return Flux.merge(monos).all(value -> value);
    }

    public Mono<Boolean> updateOnlineUserStatus(@NotNull Long userId, @NotNull UserStatus userStatus) {
        if (userStatus == UserStatus.UNRECOGNIZED || userStatus == UserStatus.OFFLINE) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        if (turmsClusterManager.isCurrentNodeResponsibleByUserId(userId)) {
            OnlineUserManager manager = getLocalOnlineUserManager(userId);
            if (manager != null) {
                return Mono.just(manager.setUserOnlineStatus(userStatus));
            } else {
                return Mono.just(false);
            }
        } else {
            Member member = turmsClusterManager.getMemberByUserId(userId);
            UpdateOnlineUserStatusTask task = new UpdateOnlineUserStatusTask(userId, userStatus.getNumber());
            Future<Boolean> future = turmsClusterManager.getExecutor()
                    .submitToMember(task, member);
            return ReactorUtil.future2Mono(future);
        }
    }

    public Mono<UserOnlineInfo> queryUserOnlineInfo(@NotNull Long userId) {
        if (turmsClusterManager.isCurrentNodeResponsibleByUserId(userId)) {
            OnlineUserManager localOnlineUserManager = getLocalOnlineUserManager(userId);
            if (localOnlineUserManager != null) {
                return Mono.just(localOnlineUserManager.getOnlineUserInfo());
            } else {
                return Mono.just(UserOnlineInfo.builder()
                        .userId(userId)
                        .userStatus(UserStatus.OFFLINE)
                        .build());
            }
        } else {
            Member member = turmsClusterManager.getMemberByUserId(userId);
            QueryUserOnlineInfoTask task = new QueryUserOnlineInfoTask(userId);
            Future<UserOnlineInfo> future = turmsClusterManager.getExecutor()
                    .submitToMember(task, member);
            return ReactorUtil.future2Mono(future);
        }
    }

    public Flux<UserOnlineInfo> queryUserOnlineInfos(@NotNull Integer number) {
        Pair<Integer, Integer> workingRange = turmsClusterManager.getWorkingRange();
        if (workingRange == null) {
            return Flux.empty();
        } else {
            Integer start = workingRange.getLeft();
            Integer end = workingRange.getRight();
            return Flux.create(sink -> {
                // Do not use Flux.take()
                int count = 0;
                for (int i = start; i < end; i++) {
                    if (count >= number) {
                        break;
                    }
                    Map<Long, OnlineUserManager> map = onlineUsersManagerAtSlots.get(i);
                    if (map != null && !map.isEmpty()) {
                        for (OnlineUserManager manager : map.values()) {
                            if (count >= number) {
                                break;
                            }
                            sink.next(manager.getOnlineUserInfo());
                            count++;
                        }
                    }
                }
                sink.complete();
            });
        }
    }

    private void onClusterMembersChange() {
        setIrresponsibleUsersOffline(false);
    }

    public SortedSet<UserLocation> getUserLocations(@NotNull Long userId, @DeviceTypeConstraint DeviceType deviceType) {
        if (usersNearbyService.isTreatUserIdAndDeviceTypeAsUniqueUser()) {
            if (deviceType != null) {
                return usersNearbyService.getUserSessionLocations().get(Pair.of(userId, deviceType));
            } else {
                throw new IllegalArgumentException("deviceType must be not null if treatUserIdAndDeviceTypeAsUniqueUser is true");
            }
        } else {
            return usersNearbyService.getUserLocations().get(userId);
        }
    }

    public boolean updateUserLocation(
            @NotNull Long userId,
            @NotNull DeviceType deviceType,
            float latitude,
            float longitude,
            @Nullable String name,
            @Nullable String address) {
        OnlineUserManager onlineUserManager = getLocalOnlineUserManager(userId);
        if (onlineUserManager != null) {
            Date now = new Date();
            UserLocation location = new UserLocation(
                    turmsClusterManager.generateRandomId(),
                    userId,
                    deviceType,
                    longitude,
                    latitude,
                    name,
                    address,
                    now);
            OnlineUserManager.Session session = onlineUserManager.getSession(deviceType);
            if (session != null) {
                session.setLocation(location);
                userLocationService.saveUserLocation(location).subscribe();
                return true;
            }
        }
        return false;
    }

    public Integer getDisconnectionReason(@NotNull Long userId, @NotNull String sessionId) {
        if (turmsClusterManager.getTurmsProperties().getSession().isEnableQueryLoginFailedReason()) {
            return disconnectionReasonCache.getIfPresent(Pair.of(userId, sessionId));
        } else {
            return null;
        }
    }
}
