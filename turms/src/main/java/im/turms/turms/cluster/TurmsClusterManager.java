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

package im.turms.turms.cluster;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipAdapter;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.config.Config;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.ReplicatedMapConfig;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.cluster.HazelcastConfig;
import im.turms.turms.common.TurmsLogger;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.task.QueryResponsibleTurmsServerAddressTask;
import im.turms.turms.task.TurmsTaskExecutor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;

@Component
public class TurmsClusterManager {
    @Value("${server.port}")
    Integer port;

    public static final int HASH_SLOTS_NUMBER = 127;
    private static final String HAZELCAST_KEY_SHARED_PROPERTIES = "sharedProperties";
    private static final String HAZELCAST_KEY_DEFAULT = "default";
    private static final String CLUSTER_STATE = "clusterState";
    private static final String CLUSTER_TIME = "clusterTime";
    private static final String CLUSTER_VERSION = "clusterVersion";
    private static final String MEMBERS = "members";
    @Getter
    @Setter
    private HazelcastInstance hazelcastInstance;
    private TurmsProperties sharedTurmsProperties;
    private ReplicatedMap<SharedPropertiesKey, Object> sharedProperties;
    private List<Member> membersSnapshot = Collections.emptyList();
    private Member localMembersSnapshot;
    private boolean isMaster = false;
    private boolean hasJoinedCluster = false;
    private FlakeIdGenerator idGenerator;
    private final List<Function<MembershipEvent, Void>> onMembersChangeListeners;
    private final Cache<UUID, String> memberAddressCache;
    private final TurmsTaskExecutor turmsTaskExecutor;
    @Getter
    private String localTurmsServerAddress;

    public TurmsClusterManager(
            TurmsProperties localTurmsProperties,
            @Lazy HazelcastInstance hazelcastInstance,
            @Lazy TurmsTaskExecutor turmsTaskExecutor) {
        sharedTurmsProperties = localTurmsProperties;
        onMembersChangeListeners = new LinkedList<>();
        this.hazelcastInstance = hazelcastInstance;
        memberAddressCache = Caffeine
                .newBuilder()
                .maximumSize(HASH_SLOTS_NUMBER)
                .build();
        this.turmsTaskExecutor = turmsTaskExecutor;
    }

    @HazelcastConfig
    public Function<Config, Void> clusterListenerConfig() {
        return config -> {
            ReplicatedMapConfig replicatedMapConfig = config.getReplicatedMapConfig(HAZELCAST_KEY_SHARED_PROPERTIES);
            replicatedMapConfig.addEntryListenerConfig(
                    new EntryListenerConfig().setImplementation(sharedPropertiesEntryListener()));
            config.addListenerConfig(new ListenerConfig(new MembershipAdapter() {
                @Override
                public void memberAdded(MembershipEvent membershipEvent) {
                    membersSnapshot = new ArrayList<>(hazelcastInstance.getCluster().getMembers());
                    memberAddressCache.invalidateAll();
                    if (membersSnapshot.size() > HASH_SLOTS_NUMBER) {
                        shutdown();
                        throw new RuntimeException("The members of cluster should be not more than " + HASH_SLOTS_NUMBER);
                    }
                    localMembersSnapshot = hazelcastInstance.getCluster().getLocalMember();
                    localTurmsServerAddress = String.format("%s:%d",
                            localMembersSnapshot.getAddress().getHost(),
                            port);
                    if (!hasJoinedCluster) {
                        initEnvAfterJoinedCluster();
                    }
                    hasJoinedCluster = true;
                    if (isCurrentMemberMaster()) {
                        if (!isMaster && membersSnapshot.size() > 1) {
                            uploadPropertiesToAllMembers();
                        }
                        isMaster = true;
                    } else {
                        isMaster = false;
                    }
                    logWorkingRanges(
                            membershipEvent.getCluster().getMembers(),
                            membershipEvent.getCluster().getLocalMember());
                    notifyMembersChangeListeners(membershipEvent);
                }

                @Override
                public void memberRemoved(MembershipEvent membershipEvent) {
                    membersSnapshot = new ArrayList<>(hazelcastInstance.getCluster().getMembers());
                    memberAddressCache.invalidateAll();
                    localMembersSnapshot = hazelcastInstance.getCluster().getLocalMember();
                    logWorkingRanges(
                            membershipEvent.getCluster().getMembers(),
                            membershipEvent.getCluster().getLocalMember());
                    hasJoinedCluster = hasJoinedCluster();
                    notifyMembersChangeListeners(membershipEvent);
                }
            }));
            return null;
        };
    }

    public boolean isServing() {
        return hazelcastInstance.getLifecycleService().isRunning() &&
                hasJoinedCluster &&
                sharedTurmsProperties.getCluster().getMinimumQuorumToServe() <= membersSnapshot.size();
    }

    public boolean isCurrentMemberMaster() {
        Iterator<Member> iterator = hazelcastInstance.getCluster().getMembers().iterator();
        if (iterator.hasNext()) {
            return iterator.next().getUuid()
                    .equals(hazelcastInstance.getCluster().getLocalMember().getUuid());
        } else {
            return false;
        }
    }

    private void logWorkingRanges(@NotEmpty Set<Member> members, @NotNull Member localMember) {
        int step = HASH_SLOTS_NUMBER / members.size();
        Map<Object, Object> result = new HashMap<>();
        Member[] clusterMembers = members.toArray(new Member[0]);
        for (int index = 0; index < clusterMembers.length; index++) {
            int start = index * step;
            int end = index == clusterMembers.length - 1
                    ? HASH_SLOTS_NUMBER
                    : (index + 1) * step;
            String range = "[" + start + "," + end + ")";
            boolean isCurrentNodeRange = localMember == clusterMembers[index];
            result.put(index, range + (isCurrentNodeRange ? "*" : ""));
        }
        TurmsLogger.logJson("Working Ranges for Slot Indexes", result);
    }

    private void initEnvAfterJoinedCluster() {
        if (hazelcastInstance != null) {
            idGenerator = hazelcastInstance.getFlakeIdGenerator(HAZELCAST_KEY_DEFAULT);
            sharedProperties = hazelcastInstance.getReplicatedMap(HAZELCAST_KEY_SHARED_PROPERTIES);
            getTurmsPropertiesFromCluster();
        }
    }

    private boolean hasJoinedCluster() {
        return membersSnapshot.indexOf(localMembersSnapshot) != -1;
    }

    public IScheduledExecutorService getScheduledExecutor() {
        return hazelcastInstance.getScheduledExecutorService(HAZELCAST_KEY_DEFAULT);
    }

    public IExecutorService getExecutor() {
        return hazelcastInstance.getExecutorService(HAZELCAST_KEY_DEFAULT);
    }

    public void getTurmsPropertiesFromCluster() {
        Object turmsPropertiesObject = sharedProperties.get(SharedPropertiesKey.TURMS_PROPERTIES);
        if (turmsPropertiesObject instanceof TurmsProperties) {
            sharedTurmsProperties = (TurmsProperties) turmsPropertiesObject;
        }
    }

    public void uploadPropertiesToAllMembers() {
        sharedProperties.put(SharedPropertiesKey.TURMS_PROPERTIES, sharedTurmsProperties);
    }

    public void updatePropertiesAndNotify(@NotNull TurmsProperties properties) {
        sharedProperties.put(SharedPropertiesKey.TURMS_PROPERTIES, properties);
        TurmsProperties.notifyListeners(properties);
    }

    private EntryAdapter<SharedPropertiesKey, Object> sharedPropertiesEntryListener() {
        return new EntryAdapter<>() {
            @Override
            public void entryUpdated(EntryEvent<SharedPropertiesKey, Object> event) {
                onSharedPropertiesAddedOrUpdated(event);
            }

            @Override
            public void entryAdded(EntryEvent<SharedPropertiesKey, Object> event) {
                onSharedPropertiesAddedOrUpdated(event);
            }
        };
    }

    private void onSharedPropertiesAddedOrUpdated(EntryEvent<SharedPropertiesKey, Object> event) {
        switch (event.getKey()) {
            case TURMS_PROPERTIES:
                sharedTurmsProperties = (TurmsProperties) event.getValue();
                break;
            default:
                break;
        }
    }

    public void addListenerOnMembersChange(Function<MembershipEvent, Void> listener) {
        onMembersChangeListeners.add(listener);
    }

    private void notifyMembersChangeListeners(MembershipEvent membershipEvent) {
        for (Function<MembershipEvent, Void> function : onMembersChangeListeners) {
            function.apply(membershipEvent);
        }
    }

    public Map<String, Object> getHazelcastInfo(boolean withConfigs) {
        Map<String, Object> map = new HashMap<>(4);
        map.put(CLUSTER_STATE, hazelcastInstance.getCluster().getClusterState());
        map.put(CLUSTER_TIME, hazelcastInstance.getCluster().getClusterTime());
        map.put(CLUSTER_VERSION, hazelcastInstance.getCluster().getClusterVersion());
        map.put(MEMBERS, hazelcastInstance.getCluster().getMembers());
        if (withConfigs) {
            map.put("configs", hazelcastInstance.getConfig());
        }
        return map;
    }

    public TurmsProperties getTurmsProperties() {
        return sharedTurmsProperties;
    }

    /**
     * Note: It's unnecessary to check if the ID is 0L because of its mechanism
     */
    public Long generateRandomId() {
        return idGenerator.newId();
    }

    public boolean isCurrentNodeResponsibleByUserId(@NotNull Long userId) {
        int index = getSlotIndexByUserId(userId);
        Member member = getClusterMemberBySlotIndex(index);
        return member != null && member.getUuid().equals(localMembersSnapshot.getUuid());
    }

    public boolean isCurrentNodeResponsibleBySlotIndex(@NotNull Integer slotIndex) {
        Member member = getClusterMemberBySlotIndex(slotIndex);
        return member != null && member.getUuid().equals(localMembersSnapshot.getUuid());
    }

    public Member getLocalMember() {
        return localMembersSnapshot;
    }

    public Integer getLocalMemberIndex() {
        Member localMember = getLocalMember();
        int index = membersSnapshot.indexOf(localMember);
        return index != -1 ? index : null;
    }

    public Set<Member> getMembers() {
        return hazelcastInstance.getCluster().getMembers();
    }

    public Member getClusterMemberBySlotIndex(@NotNull Integer slotIndex) {
        if (slotIndex >= 0 && slotIndex < HASH_SLOTS_NUMBER) {
            int count = membersSnapshot.size();
            if (count == 0) {
                return null;
            }
            int memberIndex = slotIndex / (HASH_SLOTS_NUMBER / count);
            if (memberIndex < count) {
                return membersSnapshot.get(memberIndex);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * [start, end)
     */
    public Pair<Integer, Integer> getWorkingRange() {
        int size = membersSnapshot.size();
        if (size != 0) {
            Integer localMemberIndex = getLocalMemberIndex();
            if (localMemberIndex != null) {
                int step = HASH_SLOTS_NUMBER / size;
                return Pair.of(localMemberIndex * step,
                        (localMemberIndex + 1) * step);
            }
        }
        return null;
    }

    public Member getMemberByUserId(@NotNull Long userId) {
        int index = getSlotIndexByUserId(userId);
        return getClusterMemberBySlotIndex(index);
    }

    public int getSlotIndexByUserId(@NotNull Long userId) {
        return (int) (userId % HASH_SLOTS_NUMBER);
    }

    public Integer getSlotIndexByUserIdForCurrentNode(@NotNull Long userId) {
        int slotIndex = getSlotIndexByUserId(userId);
        Member member = getClusterMemberBySlotIndex(slotIndex);
        return member != null && member == getLocalMember() ? slotIndex : null;
    }

    public Mono<String> getResponsibleTurmsServerAddress(@NotNull Long userId) {
        Validator.throwIfAnyNull(userId);
        Member member = getMemberByUserId(userId);
        if (member == null) {
            return Mono.empty();
        } else {
            if (member.getUuid().equals(getLocalMember().getUuid())) {
                return Mono.just(getLocalTurmsServerAddress());
            } else {
                String address = memberAddressCache.getIfPresent(member.getUuid());
                if (address != null) {
                    return Mono.just(address);
                } else {
                    return turmsTaskExecutor.call(member,
                            new QueryResponsibleTurmsServerAddressTask())
                            .doOnNext(addr -> memberAddressCache.put(
                                    member.getUuid(), addr));
                }
            }
        }
    }

    public void shutdown() {
        if (hazelcastInstance != null && hazelcastInstance.getLifecycleService().isRunning()) {
            hazelcastInstance.shutdown();
        }
    }

    public boolean isSingleton() {
        return membersSnapshot.size() == 1;
    }

    private enum SharedPropertiesKey {
        TURMS_PROPERTIES,
    }
}