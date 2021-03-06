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
// source: model/group/group_invitation.proto

package im.turms.common.model.bo.group;

public final class GroupInvitationOuterClass {
  private GroupInvitationOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_GroupInvitation_descriptor;
  static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_GroupInvitation_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\"model/group/group_invitation.proto\022\016im" +
      ".turms.proto\032\036google/protobuf/wrappers.p" +
      "roto\032\035constant/request_status.proto\"\223\003\n\017" +
      "GroupInvitation\022\'\n\002id\030\001 \001(\0132\033.google.pro" +
      "tobuf.Int64Value\0222\n\rcreation_date\030\002 \001(\0132" +
      "\033.google.protobuf.Int64Value\022-\n\007content\030" +
      "\003 \001(\0132\034.google.protobuf.StringValue\022-\n\006s" +
      "tatus\030\004 \001(\0162\035.im.turms.proto.RequestStat" +
      "us\0224\n\017expiration_date\030\005 \001(\0132\033.google.pro" +
      "tobuf.Int64Value\022-\n\010group_id\030\006 \001(\0132\033.goo" +
      "gle.protobuf.Int64Value\022/\n\ninviter_id\030\007 " +
      "\001(\0132\033.google.protobuf.Int64Value\022/\n\ninvi" +
      "tee_id\030\010 \001(\0132\033.google.protobuf.Int64Valu" +
      "eB%\n\036im.turms.common.model.bo.groupP\001\272\002\000" +
      "b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
          im.turms.common.constant.RequestStatusOuterClass.getDescriptor(),
        });
    internal_static_im_turms_proto_GroupInvitation_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_GroupInvitation_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_GroupInvitation_descriptor,
        new java.lang.String[] { "Id", "CreationDate", "Content", "Status", "ExpirationDate", "GroupId", "InviterId", "InviteeId", });
    com.google.protobuf.WrappersProto.getDescriptor();
    im.turms.common.constant.RequestStatusOuterClass.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
