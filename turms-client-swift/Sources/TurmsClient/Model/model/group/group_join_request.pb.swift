// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: model/group/group_join_request.proto
//
// For information on using the generated types, please see the documentation:
//   https://github.com/apple/swift-protobuf/

import Foundation
import SwiftProtobuf

// If the compiler emits an error on this type, it is because this file
// was generated by a version of the `protoc` Swift plug-in that is
// incompatible with the version of SwiftProtobuf to which you are linking.
// Please ensure that your are building against the same version of the API
// that was used to generate this file.
fileprivate struct _GeneratedWithProtocGenSwiftVersion: SwiftProtobuf.ProtobufAPIVersionCheck {
  struct _2: SwiftProtobuf.ProtobufAPIVersion_2 {}
  typealias Version = _2
}

public struct GroupJoinRequest {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  public var id: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._id ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._id = newValue}
  }
  /// Returns true if `id` has been explicitly set.
  public var hasID: Bool {return _storage._id != nil}
  /// Clears the value of `id`. Subsequent reads from it will return its default value.
  public mutating func clearID() {_uniqueStorage()._id = nil}

  public var creationDate: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._creationDate ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._creationDate = newValue}
  }
  /// Returns true if `creationDate` has been explicitly set.
  public var hasCreationDate: Bool {return _storage._creationDate != nil}
  /// Clears the value of `creationDate`. Subsequent reads from it will return its default value.
  public mutating func clearCreationDate() {_uniqueStorage()._creationDate = nil}

  public var content: SwiftProtobuf.Google_Protobuf_StringValue {
    get {return _storage._content ?? SwiftProtobuf.Google_Protobuf_StringValue()}
    set {_uniqueStorage()._content = newValue}
  }
  /// Returns true if `content` has been explicitly set.
  public var hasContent: Bool {return _storage._content != nil}
  /// Clears the value of `content`. Subsequent reads from it will return its default value.
  public mutating func clearContent() {_uniqueStorage()._content = nil}

  public var status: RequestStatus {
    get {return _storage._status}
    set {_uniqueStorage()._status = newValue}
  }

  public var expirationDate: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._expirationDate ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._expirationDate = newValue}
  }
  /// Returns true if `expirationDate` has been explicitly set.
  public var hasExpirationDate: Bool {return _storage._expirationDate != nil}
  /// Clears the value of `expirationDate`. Subsequent reads from it will return its default value.
  public mutating func clearExpirationDate() {_uniqueStorage()._expirationDate = nil}

  public var groupID: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._groupID ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._groupID = newValue}
  }
  /// Returns true if `groupID` has been explicitly set.
  public var hasGroupID: Bool {return _storage._groupID != nil}
  /// Clears the value of `groupID`. Subsequent reads from it will return its default value.
  public mutating func clearGroupID() {_uniqueStorage()._groupID = nil}

  public var requesterID: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._requesterID ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._requesterID = newValue}
  }
  /// Returns true if `requesterID` has been explicitly set.
  public var hasRequesterID: Bool {return _storage._requesterID != nil}
  /// Clears the value of `requesterID`. Subsequent reads from it will return its default value.
  public mutating func clearRequesterID() {_uniqueStorage()._requesterID = nil}

  public var responderID: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._responderID ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._responderID = newValue}
  }
  /// Returns true if `responderID` has been explicitly set.
  public var hasResponderID: Bool {return _storage._responderID != nil}
  /// Clears the value of `responderID`. Subsequent reads from it will return its default value.
  public mutating func clearResponderID() {_uniqueStorage()._responderID = nil}

  public var unknownFields = SwiftProtobuf.UnknownStorage()

  public init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "im.turms.proto"

extension GroupJoinRequest: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  public static let protoMessageName: String = _protobuf_package + ".GroupJoinRequest"
  public static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "id"),
    2: .standard(proto: "creation_date"),
    3: .same(proto: "content"),
    4: .same(proto: "status"),
    5: .standard(proto: "expiration_date"),
    6: .standard(proto: "group_id"),
    7: .standard(proto: "requester_id"),
    8: .standard(proto: "responder_id"),
  ]

  fileprivate class _StorageClass {
    var _id: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _creationDate: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _content: SwiftProtobuf.Google_Protobuf_StringValue? = nil
    var _status: RequestStatus = .pending
    var _expirationDate: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _groupID: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _requesterID: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _responderID: SwiftProtobuf.Google_Protobuf_Int64Value? = nil

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _id = source._id
      _creationDate = source._creationDate
      _content = source._content
      _status = source._status
      _expirationDate = source._expirationDate
      _groupID = source._groupID
      _requesterID = source._requesterID
      _responderID = source._responderID
    }
  }

  fileprivate mutating func _uniqueStorage() -> _StorageClass {
    if !isKnownUniquelyReferenced(&_storage) {
      _storage = _StorageClass(copying: _storage)
    }
    return _storage
  }

  public mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    _ = _uniqueStorage()
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      while let fieldNumber = try decoder.nextFieldNumber() {
        switch fieldNumber {
        case 1: try decoder.decodeSingularMessageField(value: &_storage._id)
        case 2: try decoder.decodeSingularMessageField(value: &_storage._creationDate)
        case 3: try decoder.decodeSingularMessageField(value: &_storage._content)
        case 4: try decoder.decodeSingularEnumField(value: &_storage._status)
        case 5: try decoder.decodeSingularMessageField(value: &_storage._expirationDate)
        case 6: try decoder.decodeSingularMessageField(value: &_storage._groupID)
        case 7: try decoder.decodeSingularMessageField(value: &_storage._requesterID)
        case 8: try decoder.decodeSingularMessageField(value: &_storage._responderID)
        default: break
        }
      }
    }
  }

  public func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if let v = _storage._id {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 1)
      }
      if let v = _storage._creationDate {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 2)
      }
      if let v = _storage._content {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 3)
      }
      if _storage._status != .pending {
        try visitor.visitSingularEnumField(value: _storage._status, fieldNumber: 4)
      }
      if let v = _storage._expirationDate {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 5)
      }
      if let v = _storage._groupID {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 6)
      }
      if let v = _storage._requesterID {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 7)
      }
      if let v = _storage._responderID {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 8)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  public static func ==(lhs: GroupJoinRequest, rhs: GroupJoinRequest) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._id != rhs_storage._id {return false}
        if _storage._creationDate != rhs_storage._creationDate {return false}
        if _storage._content != rhs_storage._content {return false}
        if _storage._status != rhs_storage._status {return false}
        if _storage._expirationDate != rhs_storage._expirationDate {return false}
        if _storage._groupID != rhs_storage._groupID {return false}
        if _storage._requesterID != rhs_storage._requesterID {return false}
        if _storage._responderID != rhs_storage._responderID {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
