package im.turms.client;

import im.turms.client.driver.TurmsDriver;
import im.turms.client.service.GroupService;
import im.turms.client.service.MessageService;
import im.turms.client.service.NotificationService;
import im.turms.client.service.UserService;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class TurmsClient {
    private TurmsDriver driver;
    private UserService userService;
    private GroupService groupService;
    private MessageService messageService;

    private NotificationService notificationService;

    public TurmsClient(
            @NotNull String url,
            @Nullable Integer connectionTimeout,
            @Nullable Integer minRequestsInterval) {
        driver = new TurmsDriver(url, connectionTimeout, minRequestsInterval);
        userService = new UserService(this);
        groupService = new GroupService(this);
        messageService = new MessageService(this);
        notificationService = new NotificationService(this);
    }

    public TurmsDriver getDriver() {
        return driver;
    }

    public UserService getUserService() {
        return userService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}