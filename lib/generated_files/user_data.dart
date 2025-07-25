import 'package:freezed_annotation/freezed_annotation.dart';

part 'user_data.freezed.dart';
part 'user_data.g.dart';

@freezed
abstract class UserData with _$UserData {
  factory UserData({
    required String name,
    required String email,
    required String phone,
    required String address,
    required String city,
    required String country,
    required String postalCode,
    required String profilePictureUrl,
    required String bio,      
  }) = _UserData;

  factory UserData.fromJson(Map<String, dynamic> json) => _$UserDataFromJson(json);
}