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

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/group/enrollment/update_group_join_question_request.proto

package im.turms.common.model.dto.request.group.enrollment;

public final class UpdateGroupJoinQuestionRequestOuterClass {
  private UpdateGroupJoinQuestionRequestOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_UpdateGroupJoinQuestionRequest_descriptor;
  static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_UpdateGroupJoinQuestionRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\nArequest/group/enrollment/update_group_" +
      "join_question_request.proto\022\016im.turms.pr" +
      "oto\032\036google/protobuf/wrappers.proto\"\242\001\n\036" +
      "UpdateGroupJoinQuestionRequest\022\023\n\013questi" +
      "on_id\030\001 \001(\003\022.\n\010question\030\002 \001(\0132\034.google.p" +
      "rotobuf.StringValue\022\017\n\007answers\030\003 \003(\t\022*\n\005" +
      "score\030\004 \001(\0132\033.google.protobuf.Int32Value" +
      "B9\n2im.turms.common.model.dto.request.gr" +
      "oup.enrollmentP\001\272\002\000b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
        });
    internal_static_im_turms_proto_UpdateGroupJoinQuestionRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_UpdateGroupJoinQuestionRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_UpdateGroupJoinQuestionRequest_descriptor,
        new java.lang.String[] { "QuestionId", "Question", "Answers", "Score", });
    com.google.protobuf.WrappersProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
