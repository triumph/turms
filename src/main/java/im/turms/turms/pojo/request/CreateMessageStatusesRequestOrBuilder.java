// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/message/create_message_statuses_request.proto

package im.turms.turms.pojo.request;

public interface CreateMessageStatusesRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:im.turms.proto.CreateMessageStatusesRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int64 message_id = 1;</code>
   * @return The messageId.
   */
  long getMessageId();

  /**
   * <code>.im.turms.proto.ChatType chat_type = 2;</code>
   * @return The enum numeric value on the wire for chatType.
   */
  int getChatTypeValue();
  /**
   * <code>.im.turms.proto.ChatType chat_type = 2;</code>
   * @return The chatType.
   */
  im.turms.turms.constant.ChatType getChatType();

  /**
   * <code>int64 to_id = 3;</code>
   * @return The toId.
   */
  long getToId();

  /**
   * <code>.google.protobuf.Int32Value burn_after = 4;</code>
   * @return Whether the burnAfter field is set.
   */
  boolean hasBurnAfter();
  /**
   * <code>.google.protobuf.Int32Value burn_after = 4;</code>
   * @return The burnAfter.
   */
  com.google.protobuf.Int32Value getBurnAfter();
  /**
   * <code>.google.protobuf.Int32Value burn_after = 4;</code>
   */
  com.google.protobuf.Int32ValueOrBuilder getBurnAfterOrBuilder();
}
