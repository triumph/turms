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
// source: request/user/relationship/update_relationship_request.proto

package im.turms.common.model.dto.request.user.relationship;

public final class UpdateRelationshipRequestOuterClass {
  private UpdateRelationshipRequestOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_UpdateRelationshipRequest_descriptor;
  static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_UpdateRelationshipRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n;request/user/relationship/update_relat" +
      "ionship_request.proto\022\016im.turms.proto\032\036g" +
      "oogle/protobuf/wrappers.proto\"\310\001\n\031Update" +
      "RelationshipRequest\022\017\n\007user_id\030\001 \001(\003\022+\n\007" +
      "blocked\030\002 \001(\0132\032.google.protobuf.BoolValu" +
      "e\0224\n\017new_group_index\030\003 \001(\0132\033.google.prot" +
      "obuf.Int32Value\0227\n\022delete_group_index\030\004 " +
      "\001(\0132\033.google.protobuf.Int32ValueB:\n3im.t" +
      "urms.common.model.dto.request.user.relat" +
      "ionshipP\001\272\002\000b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
        });
    internal_static_im_turms_proto_UpdateRelationshipRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_UpdateRelationshipRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_UpdateRelationshipRequest_descriptor,
        new java.lang.String[] { "UserId", "Blocked", "NewGroupIndex", "DeleteGroupIndex", });
    com.google.protobuf.WrappersProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
