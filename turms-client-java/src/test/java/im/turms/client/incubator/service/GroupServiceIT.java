package im.turms.client.incubator.service;

import helper.ExceptionUtil;
import im.turms.client.incubator.TurmsClient;
import im.turms.client.incubator.model.GroupWithVersion;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.GroupMemberRole;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.bo.common.Int64ValuesWithVersion;
import im.turms.turms.pojo.bo.group.*;
import im.turms.turms.pojo.bo.user.UsersInfosWithVersion;
import org.junit.jupiter.api.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GroupServiceIT {
    private static final long GROUP_MEMBER_ID = 3;
    private static final long GROUP_INVITATION_INVITEE = 4;
    private static final long GROUP_SUCCESSOR = 1;
    private static final long GROUP_BLACKLISTED_USER_ID = 4;
    private static TurmsClient turmsClient;
    private static GroupService groupService;
    private static Long groupId;
    private static Long groupJoinQuestionId;
    private static Long groupJoinRequestId;
    private static Long groupInvitationId;

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        turmsClient = new TurmsClient(WS_URL, null, null);
        groupService = new GroupService(turmsClient);
        CompletableFuture<Void> future = turmsClient.getDriver().connect(1, "123", 10, null, UserStatus.BUSY, DeviceType.ANDROID);
        future.get(5, TimeUnit.SECONDS);
    }

    @AfterAll
    static void tearDown() {
        if (turmsClient.getDriver().connected()) {
            turmsClient.getDriver().disconnect();
        }
    }

    // Constructor

    @Test
    @Order(ORDER_FIRST)
    public void constructor_shouldReturnNotNullGroupServiceInstance() {
        assertNotNull(groupService);
    }

    // Create

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    public void createGroup_shouldReturnGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        groupId = groupService.createGroup("name", "intro", "announcement", null, 10, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(groupId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void addGroupJoinQuestion_shouldReturnQuestionId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = groupService.addGroupJoinQuestion(groupId, "question", List.of("answer1", "answer2"), 10);
        groupJoinQuestionId = future.get();
        assertNotNull(groupJoinQuestionId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void createJoinRequest_shouldReturnJoinRequestId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = groupService.createJoinRequest(groupId, "content");
        groupJoinRequestId = future.get();
        assertNotNull(groupJoinRequestId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void addGroupMember_shouldSucceed() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = groupService.addGroupMember(groupId, GROUP_MEMBER_ID, "name", GroupMemberRole.MEMBER, null);
        assertNull(future.get());
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void blacklistUser_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.blacklistUser(groupId, GROUP_BLACKLISTED_USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void createInvitation_shouldReturnInvitationId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = groupService.createInvitation(groupId, GROUP_INVITATION_INVITEE, "content");
        groupInvitationId = future.get();
        assertNotNull(groupInvitationId);
    }

    // Update

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.updateGroup(groupId, "name", "intro", "announcement", null, 10, null, null, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void transferOwnership_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.transferOwnership(groupId, GROUP_SUCCESSOR, true)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void muteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.muteGroup(groupId, new Date())
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void unmuteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.unmuteGroup(groupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateGroupJoinQuestion_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.updateGroupJoinQuestion(groupId, "new-question", List.of("answer"), null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateGroupMemberInfo_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.updateGroupMemberInfo(groupId, GROUP_MEMBER_ID, "myname", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void muteGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.muteGroupMember(groupId, GROUP_MEMBER_ID, new Date(System.currentTimeMillis() + 100000))
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void unmuteGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.unmuteGroupMember(groupId, GROUP_MEMBER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Query

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroup_shouldGroupWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        GroupWithVersion groupWithVersion = groupService.queryGroup(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupId, groupWithVersion.getGroup().getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryJoinedGroupsIds_shouldEqualNewGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        boolean found = false;
        Int64ValuesWithVersion joinedGroupsIdsWithVersion = groupService.queryJoinedGroupsIds(null)
                .get(5, TimeUnit.SECONDS);
        for (Long joinedGroupId : joinedGroupsIdsWithVersion.getValuesList()) {
            if (groupId.equals(joinedGroupId)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryJoinedGroupsInfos_shouldEqualNewGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        boolean found = false;
        GroupsWithVersion groupWithVersion = groupService.queryJoinedGroupsInfos(null)
                .get(5, TimeUnit.SECONDS);
        List<Group> groupsList = groupWithVersion.getGroupsList();
        for (Group group : groupsList) {
            if (groupId.equals(group.getId().getValue())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryBlacklistedUsersIds_shouldEqualBlacklistedUserId() throws ExecutionException, InterruptedException, TimeoutException {
        Int64ValuesWithVersion blacklistedUsersIdsWithVersion = groupService.queryBlacklistedUsersIds(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_BLACKLISTED_USER_ID, blacklistedUsersIdsWithVersion.getValues(0));
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryBlacklistedUsersInfos_shouldEqualBlacklistedUserId() throws ExecutionException, InterruptedException, TimeoutException {
        UsersInfosWithVersion usersInfosWithVersion = groupService.queryBlacklistedUsersInfos(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_BLACKLISTED_USER_ID, usersInfosWithVersion.getUserInfos(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryInvitations_shouldEqualNewInvitationId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupInvitationsWithVersion groupInvitationsWithVersion = groupService.queryInvitations(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupInvitationId, groupInvitationsWithVersion.getGroupInvitations(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryJoinRequests_shouldEqualNewJoinRequestId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupJoinRequestsWithVersion groupJoinRequestsWithVersion = groupService.queryJoinRequests(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupJoinRequestId, groupJoinRequestsWithVersion.getGroupJoinRequests(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroupJoinQuestionsRequest_shouldEqualNewGroupQuestionId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupJoinQuestionsWithVersion groupJoinQuestionsWithVersion = groupService.queryGroupJoinQuestionsRequest(groupId, true, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupJoinQuestionId, groupJoinQuestionsWithVersion.getGroupJoinQuestions(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroupMembers_shouldEqualNewMemberId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupMembersWithVersion groupMembersWithVersion = groupService.queryGroupMembers(groupId, true, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_MEMBER_ID, groupMembersWithVersion.getGroupMembers(1).getUserId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroupMembersByMembersIds_shouldEqualNewMemberId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupMembersWithVersion groupMembersWithVersion = groupService.queryGroupMembersByMembersIds(groupId, List.of(GROUP_MEMBER_ID), true)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_MEMBER_ID, groupMembersWithVersion.getGroupMembers(0).getUserId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void answerGroupQuestions_shouldEqualNewQuestionId() throws ExecutionException, InterruptedException, TimeoutException {
        HashMap<Long, String> map = new HashMap<>();
        map.put(groupJoinQuestionId, "answer");
        boolean isSuccess = false;
        try {
            isSuccess = groupService.answerGroupQuestions(map)
                    .get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            isSuccess = ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.ALREADY_GROUP_MEMBER);
        }
        assertTrue(isSuccess);
    }

    // Delete

    @Test
    @Order(ORDER_LOW_PRIORITY)
    public void removeGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.removeGroupMember(groupId, GROUP_MEMBER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void deleteGroupJoinQuestion_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.deleteGroupJoinQuestion(groupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void unblacklistUser_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.unblacklistUser(groupId, GROUP_BLACKLISTED_USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void deleteInvitation_shouldSucceedOrThrowDisabledFunction() throws InterruptedException, TimeoutException {
        boolean isSuccess;
        try {
            groupService.deleteInvitation(groupInvitationId)
                    .get(5, TimeUnit.SECONDS);
            isSuccess = true;
        } catch (ExecutionException e) {
            isSuccess = ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.DISABLED_FUNCTION);
        }
        assertTrue(isSuccess);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void deleteJoinRequest_shouldSucceedOrThrowDisabledFunction() throws InterruptedException, TimeoutException {
        boolean isSuccess;
        try {
            groupService.deleteJoinRequest(groupJoinRequestId)
                    .get(5, TimeUnit.SECONDS);
            isSuccess = true;
        } catch (ExecutionException e) {
            isSuccess = ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.DISABLED_FUNCTION);
        }
        assertTrue(isSuccess);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void quitGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = groupService.quitGroup(groupId, null, false)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LAST)
    public void deleteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Long> readyToDeleteGroup = groupService.createGroup("readyToDelete", null, null, null, null, null, null);
        Long readyToDeleteGroupId = readyToDeleteGroup.get();
        Void result = groupService.deleteGroup(readyToDeleteGroupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }
}
