// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/group/enrollment/check_group_join_question_answer_request.proto

package im.turms.turms.pojo.request;

public interface CheckGroupJoinQuestionAnswerRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:im.turms.proto.CheckGroupJoinQuestionAnswerRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int64 question_id = 1;</code>
   * @return The questionId.
   */
  long getQuestionId();

  /**
   * <code>string answer = 2;</code>
   * @return The answer.
   */
  java.lang.String getAnswer();
  /**
   * <code>string answer = 2;</code>
   * @return The bytes for answer.
   */
  com.google.protobuf.ByteString
      getAnswerBytes();
}